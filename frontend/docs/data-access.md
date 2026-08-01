# Data access

How the application talks to the API. Every call is a GET; nothing is ever written back.

The decision behind this design is [ADR 0010](../../docs/adr/0010-signal-based-zoneless-frontend.md).

## Endpoint catalogue

Every endpoint is declared once in `core/http/api-endpoints.ts` and resolved against `environment.apiBaseUrl`.

```typescript
export const API_ENDPOINTS = {
  players: `${environment.apiBaseUrl}/players`,
  playerDetails: (playerId: number) => `${environment.apiBaseUrl}/players/${playerId}`,
  playerMatches: (playerId: number) => `${environment.apiBaseUrl}/players/${playerId}/matches`,
  seasons: `${environment.apiBaseUrl}/seasons`,
  currentChallenges: `${environment.apiBaseUrl}/challenges/current`,
  currentRanking: `${environment.apiBaseUrl}/rankings/current`,
  rankingHistory: `${environment.apiBaseUrl}/rankings/history`,
} as const;
```

A URL literal never appears in a service. That keeps the consumed contract describable in one file and the base URL
configurable per build.

The application consumes seven of the API's routes; the administrative ones are not reachable from the UI, since the
frontend holds no admin key.

## Two kinds of resource

Every data-access service is a `@Service` exposing `httpResource`. The distinction between the two forms is
deliberate and consistent.

### Shared resources - a field

Data that is the same for everyone is a **field** on the service, so every consumer reads the same in-flight request
rather than each triggering its own.

```typescript
@Service()
export class PlayersApi {
  public readonly players = httpResource<readonly PlayerSummary[]>(
    () => API_ENDPOINTS.players,
    { defaultValue: [] },
  );
}
```

| Service         | Field      | Endpoint                   |
| --------------- | ---------- | -------------------------- |
| `PlayersApi`    | `players`  | `GET /api/players`         |
| `SeasonsApi`    | `seasons`  | `GET /api/seasons`         |
| `ChallengesApi` | `current`  | `GET /api/challenges/current` |
| `RankingApi`    | `current`  | `GET /api/rankings/current` |

This matters most on the overview, where `Podium`, `TeamProgress`, `WeeklyChallenges` and `WeeklyRanking` all need the
current challenges and the current ranking. Four components, two requests - and each component reads the shared service
directly rather than reaching into a sibling's internals, so none of them depends on another.

### Parameterized resources - a method

Data that depends on the caller is a **factory method** taking `Signal` arguments. Changing a signal re-issues the
request; there is no subscription to manage and no manual refetch to remember.

```typescript
public history(
  playerId: Signal<number>,
  page: Signal<number>,
  gameMode: Signal<GameMode | null>,
  seasonId: Signal<number | null>,
): HttpResourceRef<PageResponse<Match> | undefined> {
  return httpResource<PageResponse<Match>>(() => ({
    url: API_ENDPOINTS.playerMatches(playerId()),
    params: {
      page: page(),
      size: MATCH_HISTORY_PAGE_SIZE,
      ...(gameMode() ? { gameMode: gameMode() } : {}),
      ...(seasonId() !== null ? { seasonId: seasonId() } : {}),
    },
  }));
}
```

| Service        | Method                              | Endpoint                            |
| -------------- | ----------------------------------- | ----------------------------------- |
| `PlayersApi`   | `details(id, gameMode, seasonId)`   | `GET /api/players/{id}`             |
| `MatchesApi`   | `history(playerId, page, gameMode, seasonId)` | `GET /api/players/{id}/matches` |
| `RankingApi`   | `history(page)`                     | `GET /api/rankings/history`         |

**Optional parameters are spread in conditionally.** A `null` filter means "no filter on this field", and omitting the
query parameter entirely is what expresses that to the backend - sending `gameMode=null` would be a 400.

Page sizes are constants owned by the service that issues the request: 10 for match history, 5 for ranking history.

## Reading a resource safely

`Resource.value()` **throws** once a resource settles into an error state - including resources declared with a
`defaultValue`. A computed reading it directly would break navigation the moment the backend is unreachable, not merely
show an error state.

`core/http/resource-state.utils.ts` provides the guards. Every computed and template expression deriving from a
resource goes through one of them.

```typescript
resourceValue(resource, fallback)  // value() when hasValue(), fallback otherwise
anyLoading(...resources)           // is any of them still loading?
anyError(...resources)             // did any of them fail?
reloadAll(...resources)            // retry all of them
```

`anyError` and `reloadAll` are counterparts: a view is only as healthy as its least healthy dependency, so one failed
request is enough to show the error state - and a retry must reload every resource, since the view cannot tell which
one failed.

An inline `resource.hasValue() ? resource.value() : fallback` is equivalent and used where the fallback is
screen-specific. What is never acceptable is an unguarded `value()`.

## Rendering resource state

`ResourceState` renders the loading, error, empty and content states for every screen, so they cannot drift apart. A
call site supplies already-translated text and projects a skeleton shaped like the content it stands in for; without a
skeleton the loading state is visually blank, though it stays announced to assistive technology.

```html
<app-resource-state
  [isLoading]="isLoading()"
  [hasError]="hasError()"
  [isEmpty]="rows().length === 0"
  (retry)="reload()"
>
  <div skeleton>…</div>
  …content…
</app-resource-state>
```

## Two pagination patterns

Both exist because the screens need different things.

**Explicit pages** - ranking history. A `page` signal drives the resource, and `Pagination` moves it. The component
owns nothing but the index; `Pagination` clamps the bounds.

**Accumulating pages** - match history. The player profile advances `page` when a sentinel element scrolls into view,
and accumulates results into a separate `matches` signal: reset to the fetched content on page zero (a fresh query
after a filter change), appended to on every later page. The accumulation cannot be derived from the resource, because
the resource only ever holds one page at a time.

`hasMoreMatches` guards both the load trigger and the sentinel element's existence - without it, reaching the end of a
fully loaded list would keep requesting a page past the last one.

## Models

`core/**/[*].model.ts` files mirror the backend DTOs as `interface` declarations, with `readonly` members. They are
hand-written rather than generated: the contract is small, and a generator would be a build-time coupling between the
two modules that [ADR 0001](../../docs/adr/0001-monorepo.md) deliberately avoids.

`PageResponse<T>` mirrors the backend's paginated envelope and is shared by both paginated screens.

When a backend DTO changes, the mirroring interface changes in the same commit. Nothing detects a drift automatically -
that is the cost of hand-written models, and it is why the API is documented in
[`backend/docs/api.md`](../../backend/docs/api.md).

## Formatting and derived display

The frontend never recomputes a value the backend already sent. Win rates, KDA, ACS, ADR, headshot percentages,
challenge percentages and ranking positions all arrive computed.

What the frontend does own is presentation, and it lives in `core/**/*.utils.ts` as pure functions:

| Concern                | Functions                                                                        |
| ---------------------- | -------------------------------------------------------------------------------- |
| Number formatting      | `formatWinRate`, `formatKda`, `formatHeadshotPercentage`, `formatScore`           |
| Identity               | `extractRiotTag`, `resolveAgentInitial`, `resolvePlayerAvatarUrl`                 |
| Rank                   | `resolveCompetitiveTierVisual`, `resolveCompetitiveTierIconUrl`, `resolveTierOrdinal` |
| Conditional styling    | `resolveWinRateVisual`, `resolveKdaVisual`, `resolveResultAccentClass`, `resolvePositionBadgeClass` |
| Challenges             | `resolveChallengeVisual`, `resolveChallengeMetricLabel`                           |
| Dates                  | `isoWeekNumber`, `formatDateRange`, `remainingWeekTime`, `formatLocalDate`, `formatLocalTime` |

They are pure functions rather than pipes because they are called from `computed()` view-model mappings, not from
templates.

`resolveTierOrdinal` deserves a note: it exists because the players list sorts by competitive rank, and rank rating
resets per tier - comparing ratings across tiers would be meaningless, so the tier is compared first and the rating
only breaks ties within it.

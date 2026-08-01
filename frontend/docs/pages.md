# Pages

The four routed screens: what each one shows, what it fetches, and the behavior worth knowing before changing it.

Every page is lazy-loaded, hosts `PAGE_LAYOUT_CLASS` unless noted, renders its states through `ResourceState`, and
reads formatted values from the API rather than recomputing them. Mockups live in [`images/`](images); screenshots of
what is actually built live in [`preview/`](preview).

| Route          | Screen                            | Mockup                     | Preview                    |
| -------------- | --------------------------------- | -------------------------- | -------------------------- |
| `''`           | [Overview](#overview)             | `overview.png`             | `overview.png`             |
| `players`      | [Players](#players)               | `player-list.png`          | `player-list.png`          |
| `players/:id`  | [Player profile](#player-profile) | `player-profile.png`       | -                          |
| `ranking`      | [Ranking history](#ranking-history) | `ranking-history.png`    | -                          |
| `**`           | Not found                         | -                          | -                          |

`player-comparison.png` exists as a mockup with no implemented screen behind it.

---

## Overview

**Route** `''` · **Component** `Overview` · **Title key** `overview.title`

The landing page, and the answer to "who is winning this week". It is presented as three full-height, scroll-snapped
sections rather than a scrolling stack, with a dot rail letting desktop users jump straight to one.

| Section       | Contents                                                   |
| ------------- | ---------------------------------------------------------- |
| Hero          | `Podium` and `TeamProgress` side by side                    |
| Challenges    | `WeeklyChallenges`                                          |
| Ranking       | `WeeklyRanking`                                             |

### Data

| Resource                | Endpoint                       | Read by                                          |
| ----------------------- | ------------------------------ | ------------------------------------------------ |
| `ChallengesApi.current` | `GET /api/challenges/current`  | `Overview`, `TeamProgress`, `WeeklyChallenges`   |
| `RankingApi.current`    | `GET /api/rankings/current`    | `Podium`, `TeamProgress`, `WeeklyRanking`        |

Four components, two HTTP requests. Each reads the shared service directly rather than reaching into a sibling's
internals, so the sub-components stay independent and none of them had to change when the hero was added.

### Behavior worth knowing

- **The page deliberately opts out of `PAGE_LAYOUT_CLASS`.** Every other page stacks blocks in normal document flow;
  this one owns a single full-height scroll-snap container. Its host is `relative block` so the dot rail can be
  positioned against the page rather than against the scrolling container, which keeps it fixed while the content
  scrolls beneath it.
- **One clock, one countdown.** `Overview` owns a `now` signal refreshed every 60 s by an `interval`, computes the week
  summary - ISO week number, formatted date range, remaining time - and passes it down to `TeamProgress`. Letting each
  widget tick independently would let their countdowns drift apart.
- **Section tracking uses `IntersectionObserver`**, registered in `afterNextRender` because the sections do not exist
  until the template has rendered them and the API is browser-only. A section becomes active at 55 % visibility, so the
  dot updates around mid-transition rather than at the first sliver. The observer is disconnected on destroy.
- **Scrolling to a section** honors `motion-safe:scroll-smooth` on the container rather than forcing an animation.

### Sub-components

**`Podium`** - the top three players in hexagon-framed avatars, echoing the hexagon position badge used by the ranking
table, followed by a compact strip of the remaining players. Position colors come from
`resolvePositionBadgeClass`.

**`TeamProgress`** - reframes the week as a collective goal: the share of challenges the whole group has cleared, an
avatar stack showing who has contributed at least one completion, and the time left. Combines both resources through
`anyLoading` / `anyError` and retries both on failure.

**`WeeklyChallenges`** - one vignette per challenge selected for the week, showing collective completion
(`completedPlayers` / `totalPlayers`). Each row carries its difficulty **as text** beside the name, so the tier is never
conveyed by color alone (WCAG 1.4.1).

**`WeeklyRanking`** - every player's position, score and exact progress toward each of the week's challenges. It reuses
the challenge color language from `WeeklyChallenges` so both widgets read as one system. Two layouts: a table from `lg`
up, where a column header carries the challenge's short metric label and its target once for the whole column, and
stacked cards below it, where each cell re-states the label since no header is rendered. `completed` is tracked
separately from `completionPercentage` - the percentage is clamped to 100, so a player who overshot and one who stopped
exactly on target draw the same full bar, and a composite challenge has no single target to fill.

---

## Players

**Route** `players` · **Component** `Players` · **Title key** `players.title`

Every tracked player in one table: identity, rank, win rate, KDA, headshot rate and match count.

### Data

`PlayersApi.players` → `GET /api/players`. One request, no parameters.

### Behavior worth knowing

- **Sorted by competitive rank, strongest first.** Tier is compared before rank rating, because rank rating resets per
  tier and is not comparable across them; the rating only breaks ties within a tier. `resolveTierOrdinal` supplies the
  tier ordering.
- **Rows are a view model.** `PlayerSummary` DTOs are mapped once, in a `computed()`, into `PlayerRow` - avatar URL,
  rank icon URL, translated rank label, Riot tag. Doing this in the template would re-resolve every row on each change
  detection pass.
- **Conditional colors are data, not markup.** `resolveWinRateVisual` and `resolveKdaVisual` return the text and bar
  classes for a value, so the thresholds live in one pure function rather than in a chain of template conditions.
- Each row links to the player's profile.

---

## Player profile

**Route** `players/:id` · **Component** `PlayerProfile` · **Title key** `playerProfile.title`

The deepest screen: one player's identity, current rank, aggregated statistics and filterable match history.

### Data

| Resource                                              | Endpoint                          |
| ----------------------------------------------------- | --------------------------------- |
| `PlayersApi.details(playerId, gameMode, seasonId)`    | `GET /api/players/{id}`           |
| `MatchesApi.history(playerId, page, gameMode, seasonId)` | `GET /api/players/{id}/matches` |
| `SeasonsApi.seasons`                                  | `GET /api/seasons`                |

`:id` arrives as an `input.required<string>()` through `withComponentInputBinding()`; `playerId` is its numeric form.

### Filters

| Filter    | Type                     | Default                             |
| --------- | ------------------------ | ----------------------------------- |
| Game mode | `signal<GameMode>`       | `COMPETITIVE`                       |
| Season    | `linkedSignal<number \| null>` | The current season, once seasons load |

**Game mode is never nullable.** Statistics are always scoped to one concrete mode, because an "all modes" aggregate
would mix incomparable queues - a deathmatch KDA and a competitive KDA describe different games. The options come from
`FILTERABLE_GAME_MODES`, the subset synchronization actually imports: offering Swiftplay or Escalation would only ever
yield the empty state. `OTHER` is offered despite naming no mode in particular, since it is the only way to reach
matches whose queue the backend could not classify. Every eligible mode is offered rather than only those the player
has played; narrowing further would require an extra endpoint, and an unplayed mode simply shows the empty state.

**Season is a `linkedSignal`, and this is the subtle part.** Its default is computed from the loaded seasons - the one
flagged `active`, falling back to the most recent - but an explicit user choice, including "all seasons" (`null`), must
survive re-computation. A plain signal seeded from an effect would overwrite the choice; the `linkedSignal`
`previous.source.length > 0` guard distinguishes "seasons have not loaded yet, recompute the default" from "seasons are
loaded, so this may be a deliberate choice, keep it". Checking `previous`'s mere presence would not work: it is already
truthy on the first pre-load computation, which would freeze the default at `null` forever.

`hasActiveFilters` compares against the defaults - current season, competitive - so the reset affordance only appears
when something genuinely diverges.

### Match history

Match history loads by **infinite scroll**, not by explicit pagination:

- `page` is a signal, advanced by `loadMoreMatches`;
- `matches` is a separate signal accumulating every page fetched so far - reset to the fetched content on page zero (a
  fresh query after a filter change), appended to on every later page. It cannot be derived from the resource, which
  only ever holds one page at a time;
- a sentinel element rendered after the last match is watched by an `IntersectionObserver`, which requests the next page
  when it scrolls into view;
- `hasMoreMatches` guards both the trigger and the sentinel's existence, so a fully loaded list stops requesting;
- `loadMoreMatches` is additionally guarded against firing while a page is already in flight - the observer can fire
  again before the previous request settles, for instance while the list is still short enough that the trigger stays on
  screen after a page loads;
- `isLoadingMoreMatches` (`isLoading() && page() > 0`) drives a small loading row appended under the visible matches,
  distinct from the full-page skeleton that covers the very first page.

Matches are grouped into days by `groupMatchesByDay`, each group carrying its own win/loss record. The history reads as
a series of sessions rather than a flat list, which is what lets a reader recognize an evening of play. Groups are
opened when the calendar day changes rather than built from a map, because the API already returns the page sorted and
preserving that order keeps the render in sync with the pagination. Draws, remakes and unknown results count as neither
a win nor a loss.

The same `matchDays` computed drives both the table and the card layout, so a day's record is computed once.

---

## Ranking history

**Route** `ranking` · **Component** `Ranking` · **Title key** `ranking.title`

The finalized ranking of every completed week, one week at a time, most recent first.

### Data

| Resource                | Endpoint                        | Why                        |
| ----------------------- | ------------------------------- | -------------------------- |
| `RankingApi.history(page)` | `GET /api/rankings/history`  | The weeks themselves        |
| `PlayersApi.players`    | `GET /api/players`              | Avatars, resolved by player id |

The ranking-history payload carries `playerId` and `displayName` but no portrait, so avatars are resolved from the
shared players resource into a `Map<number, string | null>` computed once per render rather than looked up per row.

### Behavior worth knowing

- **Explicit pagination**, unlike the player profile: a `page` signal drives the resource and `Pagination` moves it,
  five weeks per page. The component owns nothing but the index; `Pagination` clamps the bounds.
- **Two resources, one state.** `anyLoading` and `anyError` combine them, and `reload()` retries **both** - the view
  reports their combined state and cannot tell which one failed.
- Only finalized weeks appear here. The active week is on the overview.
- Each week is mapped into a `RankingHistoryWeekView` carrying its translated week label, formatted date range and
  display-ready rows.

---

## Not found

**Route** `**` · **Component** `NotFound` · **Title key** `notFound.title`

The wildcard route. Fetches nothing.

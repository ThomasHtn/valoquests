# REST API

Every route the backend exposes, its parameters, its response shape and how it fails. The live, always-current version
is the OpenAPI document generated at runtime:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`
- OpenAPI YAML: `http://localhost:8080/api-docs.yaml`

Use Swagger's **Authorize** action to supply `X-Admin-Key` before calling an administrative route.

## Conventions

- All responses are `application/json`, serialized in UTC.
- **Null-valued properties are omitted** (`spring.jackson.default-property-inclusion=non_null`), so an absent field
  means "no value", not "field removed".
- Pagination is **zero-based**. `page` defaults to `0`; `size` must be between `1` and `100`.
- Instants are ISO-8601 in UTC (`2026-08-01T09:00:00Z`); week identifiers are ISO dates naming a Monday.
- Every `GET /api/**` route is public. Every `/api/admin/**` route requires `X-Admin-Key`.

### Paginated envelope

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0
}
```

### Error envelope

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "code": "INVALID_ARGUMENT",
  "detail": "size must be between 1 and 100",
  "instance": "/api/players/3/matches",
  "timestamp": "2026-08-01T09:00:00Z"
}
```

`errors` is added as a field-name → message map for bean-validation failures only. The full status/code table is in
[`architecture.md`](architecture.md#error-handling).

---

## Public routes

### `GET /api/challenges/current`

The challenges selected for the active week, with collective completion.

```json
{
  "weekStart": "2026-07-27",
  "weekEnd": "2026-08-02",
  "lastSuccessfulSynchronizationAt": "2026-08-01T06:04:12Z",
  "challenges": [
    {
      "id": 41,
      "name": "Machine à kills",
      "description": "Réaliser 180 kills en Compétitif.",
      "difficulty": "EASY",
      "metric": "KILLS",
      "targetValue": 180,
      "points": 100,
      "completedPlayers": 2,
      "totalPlayers": 6,
      "completionPercentage": 33.33
    }
  ]
}
```

`completedPlayers` / `totalPlayers` describe the group, not one player. Per-player progress is on the ranking route.

---

### `GET /api/rankings/current`

The active week's ranking, including each player's exact progress toward every challenge.

```json
{
  "weekStart": "2026-07-27",
  "weekEnd": "2026-08-02",
  "calculatedAt": "2026-08-01T06:05:00Z",
  "ranking": [
    {
      "position": 1,
      "previousPosition": 3,
      "positionVariation": 2,
      "player": {
        "id": 3,
        "displayName": "Thomas",
        "portrait": "Omen",
        "competitiveTier": "PLATINUM_2",
        "rankRating": 47
      },
      "points": 350,
      "completedChallenges": 2,
      "totalChallenges": 5,
      "challengeProgress": [
        {
          "challengeId": 41,
          "challengeName": "Machine à kills",
          "metric": "KILLS",
          "currentValue": 180,
          "targetValue": 180,
          "unit": "kills",
          "completed": true
        }
      ]
    }
  ]
}
```

`positionVariation` is positive when a player moved up. `previousPosition` is null for a player entering the ranking.

---

### `GET /api/rankings/history`

Finalized weeks, most recent first.

| Parameter | Default | Constraint |
| --------- | ------- | ---------- |
| `page`    | `0`     | `>= 0`     |
| `size`    | `10`    | `1..100`   |

A `PageResponse<RankingHistoryWeekResponse>`, each entry holding `weekStart`, `weekEnd`, `finalizedAt`,
`winnerPlayerId` and a `ranking` array of `{ position, playerId, displayName, points, completedChallenges }`.

Only finalized weeks appear here. The active week is on `/api/rankings/current`.

---

### `GET /api/players`

Every tracked player's summary, for the list screen.

```json
[
  {
    "id": 3,
    "riotId": "Thomas#EUW",
    "displayName": "Thomas",
    "portrait": "Omen",
    "competitiveTier": "PLATINUM_2",
    "rankRating": 47,
    "kda": 1.24,
    "winRate": 53.85,
    "headshotPercentage": 22.10,
    "matchesPlayed": 130,
    "status": "ACTIVE",
    "lastSuccessfulSynchronizationAt": "2026-08-01T06:04:12Z"
  }
]
```

---

### `GET /api/players/{playerId}`

One player's profile with aggregated statistics, optionally scoped.

| Parameter  | In    | Default          | Notes                                            |
| ---------- | ----- | ---------------- | ------------------------------------------------ |
| `playerId` | path  | required         | Internal identifier                               |
| `seasonId` | query | every season     | Internal season identifier                        |
| `gameMode` | query | every mode       | A `GameMode` name, case-insensitive               |

Returns `id`, `riotId`, `displayName`, `portrait`, `competitiveTier`, `rankRating`,
`lastSuccessfulSynchronizationAt`, a `statistics` object, and `agents` / `maps` breakdowns.

`statistics` holds `kda`, `winRate`, `adr`, `acs`, `headshotPercentage`, `kills`, `deaths`, `assists`,
`matchesPlayed`, `wins`, `losses`, `mvps`. Each `agents` / `maps` entry holds its identifier and name plus
`matchesPlayed`, `wins`, `losses`, `winRate`, `kda`, `adr`, `acs`.

| Status | Cause                                     |
| ------ | ----------------------------------------- |
| 400    | `gameMode` is not a known mode            |
| 404    | The player does not exist                 |

---

### `GET /api/players/{playerId}/matches`

One player's match history, newest first. Filters combine.

| Parameter  | In    | Default  | Notes                                             |
| ---------- | ----- | -------- | ------------------------------------------------- |
| `page`     | query | `0`      | `>= 0`                                            |
| `size`     | query | `10`     | `1..100`                                          |
| `seasonId` | query | all      | Internal season identifier                        |
| `map`      | query | all      | Exact map name, e.g. `Ascent`                     |
| `agent`    | query | all      | Exact agent name, e.g. `Omen`                     |
| `result`   | query | all      | `WIN`, `LOSS` or `DRAW`, case-insensitive         |
| `gameMode` | query | all      | A `GameMode` name, case-insensitive               |

A `PageResponse<MatchResponse>`, each entry holding `id`, `startedAt`, `mapName`, `gameMode`, `agentName`, `result`,
`allyScore`, `enemyScore`, `kills`, `deaths`, `assists`, `kda`, `acs`, `adr`, `headshotPercentage`, `competitiveTier`
and `rankRating`.

`acs` and `adr` are absent for non round-based modes, where a per-round average is meaningless.

| Status | Cause                                                             |
| ------ | ------------------------------------------------------------------ |
| 400    | Invalid pagination, or an unknown `result` / `gameMode` value       |
| 404    | The player does not exist                                          |

Sorting is fixed: match start descending, then match identifier descending, so pagination is stable across pages.

---

### `GET /api/seasons`

Every known season, for the match-history filter.

```json
[{ "id": 8, "name": "Episode 10 Act 2", "active": true }]
```

Seasons are discovered during import, so this list reflects what has actually been synchronized.

---

## Administrative routes

All of them require `X-Admin-Key`. A missing key answers **401** with `ADMIN_KEY_MISSING`; a wrong one answers **403**
with `ADMIN_KEY_INVALID`. Both use the standard error envelope, emitted by the filter before the dispatcher rather than
by the global exception handler.

### Commands

| Route                                                    | Returns | Effect                                                     |
| -------------------------------------------------------- | ------- | ---------------------------------------------------------- |
| `POST /api/admin/synchronizations`                       | 200 `SynchronizationResponse` | Synchronizes every active player, then recalculates |
| `POST /api/admin/players/{playerId}/synchronizations`    | 200 `SynchronizationResponse` | Synchronizes one player, then recalculates          |
| `POST /api/admin/challenges/progress/recalculation`      | 204     | Rebuilds the active week's progress, then the ranking       |
| `POST /api/admin/rankings/recalculation`                 | 204     | Rebuilds the active week's ranking only                     |

Both recalculation routes read exclusively from PostgreSQL and never call Henrik. Synchronization already runs the
challenge recalculation whenever it imports a match, so these routes are repair tools: they replay the calculation
after a challenge definition changed, or after a recalculation failed at the end of a synchronization.

`SynchronizationResponse` holds `id`, `type`, `trigger`, `status`, `startedAt`, `finishedAt`, `lastAttemptAt`,
`lastSuccessfulSynchronizationAt`, `playersProcessed`, `failureCount` and `matchesImported`.

A synchronization command runs synchronously and can take a while: it is bounded by the Henrik rate limit, not by the
database. It surfaces upstream trouble faithfully - **429** when Henrik's rate limit is exceeded, **502** when Henrik
fails - and **404** when the requested player does not exist.

### Monitoring

| Route                                                   | Returns                                             |
| ------------------------------------------------------- | --------------------------------------------------- |
| `GET /api/admin/synchronizations/latest`                | `SynchronizationResponse` for the most recent run    |
| `GET /api/admin/synchronizations`                       | `PageResponse<SynchronizationResponse>`, `size` defaults to `20` |
| `GET /api/admin/synchronizations/{synchronizationId}`   | `SynchronizationDetailsResponse`                     |

`SynchronizationDetailsResponse` adds `errorMessage` and a `playerResults` array of
`{ playerId, displayName, status, pagesFetched, matchesImported, errorMessage }`.

---

## Not exposed

Some capabilities exist in the domain but deliberately have no route:

- **Creating or editing a player.** The tracked group is fixed and seeded by migration.
- **Creating or editing a challenge.** The catalogue is reference data owned by Flyway - see
  [ADR 0004](../../docs/adr/0004-challenge-rules-as-json.md).
- **Deep or historical synchronization.** Import is bounded to the current season, and no command backfills older acts -
  see [ADR 0008](../../docs/adr/0008-season-scoped-history.md).
- **Recalculating a past week.** Finalized weeks are immutable. The recalculation routes act on the active week only.

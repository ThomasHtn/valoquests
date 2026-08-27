# ValoQuests — Backend

The API and the brain. Every number ValoQuests shows is computed and persisted here; the
[frontend](../frontend/README.md) only renders what this service returns.

> New here? Start with the [root README](../README.md) — it covers the product, and its Part II walks
> through running the whole stack from scratch. This document is what you read once it is running.

## Role in the system

Five responsibilities, in the order they matter:

1. **Import** Valorant match history, MMR and account data from the external Henrik API.
2. **Score** stored matches into damage, challenge progress, bonuses and weekly rankings, under one
   barème.
3. **Turn the week** — close the previous week, settle its boss, ensure the run, draw the next boss
   and challenge pack, on a schedule.
4. **Replay the colony** — rebuild the run's whole economy (food, materials, morale, population) from
   its persisted inputs, never incrementally.
5. **Serve** all of it over a REST API, plus an administrative surface for repairing any of the above
   by hand.

## Stack

| Concern | Choice | Why it is worth knowing |
| --- | --- | --- |
| Language / runtime | Java 25, Spring Boot 4.0.6 | `switch` pattern matching and records are used freely across the domain |
| Persistence | PostgreSQL 17, Spring Data JPA | `ddl-auto=validate` — Hibernate never touches the schema |
| Migrations | Flyway | The schema's single source of truth |
| HTTP client | Spring `WebClient` | Reactive client, driven synchronously by the import pipeline |
| Mapping | MapStruct + Lombok | Entity ↔ DTO mapping is generated, not hand-written |
| API docs | springdoc-openapi 3.0.3 | Swagger UI is the live contract, behind a flag |
| Config | `spring-dotenv` | `.env` is read at startup; real environment variables win |

## Getting it running

Prerequisites: **JDK 25**, **Docker** (for PostgreSQL and the integration tests), and a **Henrik API
key**. Every command below runs from `backend/`.

```bash
cp .env.example .env      # then fill in the two values called out below
docker compose up -d      # PostgreSQL 17, `docker compose ps` to check health
./mvnw spring-boot:run    # http://localhost:8080
```

Two values have no usable default and must be set before the first run:

- `HENRIK_API_KEY` — without it the application starts, but every synchronization fails.
- `ADMIN_API_KEY` — a long random secret you invent. It is the only credential protecting
  `/api/admin/**`.

Once up:

| | URL |
| --- | --- |
| API | `http://localhost:8080/api/...` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON / YAML | `http://localhost:8080/api-docs` · `/api-docs.yaml` |
| Health | `http://localhost:8080/actuator/health` |

Both documentation routes need `API_DOCS_ENABLED=true`, which `.env.example` sets because that is what
local development wants. **Leave it off anywhere reachable**: the document maps out every
`/api/admin/**` route, its payload and its error codes. The flag drives
`springdoc.{api-docs,swagger-ui}.enabled` and the `permitAll` rule in `SecurityConfig` at once, so the
two cannot drift apart (`AdminApiSecurityIntegrationTest`).

In Swagger UI, **Authorize** takes your `ADMIN_API_KEY` and unlocks the administrative routes.

## Commands

```bash
./mvnw spring-boot:run                                # start the API
./mvnw test                                           # fast tests only, no Docker needed
./mvnw verify                                         # full gate: tests + checkstyle + spotbugs + jacoco
./mvnw -Dtest=PlayerSynchronizationServiceTest test    # one test class
./mvnw clean                                          # wipe target/

docker compose up -d                                  # PostgreSQL
docker compose ps                                     # health
docker compose logs -f postgres                       # database logs
docker compose down                                   # stop, keeping the data volume
docker compose down -v                                # stop and delete the data
```

CI runs exactly `./mvnw --batch-mode --no-transfer-progress clean verify`
([`backend-ci.yml`](../.github/workflows/backend-ci.yml)), on push and PR to `main`/`develop`
touching `backend/**`.

## Architecture

Package-by-feature under `io.github.thomashtn.valoquests`, each feature internally layered
(`controller` / `dto` / `entity` / `repository` / `service`, plus `model`, `mapper`, `exception`,
`scheduler`, `client`, `calculator`, `config` where the feature needs them). There is no
hexagonal/DDD split — layering is a convention, not an enforced boundary.

```text
io.github.thomashtn.valoquests
├── henrik/           External Henrik API client: rate limiting, retries, error taxonomy
├── synchronization/  Import orchestration — the pipeline that fills the database
├── match/            Match entities, import, season and match-history queries
├── player/           Player profiles, roster lifecycle, Riot account resolution
├── challenge/        Challenge catalogue, weekly selection, progress calculators
├── boss/             Boss catalogue, weekly encounter, calibration, chronology
├── scoring/          The single damage barème and its aggregators
├── ranking/          Weekly ranking query and recalculation
├── run/              The ten-week run the campaign is bounded by
├── colony/           The squad's shared colony: rules, replay engine, read models
├── week/             WeekCalendar and the weekly rollover
├── maintenance/      Destructive administrative operations
└── shared/           Security, scheduling, persistence, OpenAPI, error handling, base entity
```

### The pieces worth understanding first

**`week/WeekCalendar`** — the single decision about where a week starts. A week runs Monday 00:00 to
Monday 00:00 in `app.scheduling.week-rollover-zone`, and is identified by that Monday's `LocalDate`.
There is no `week` table. Challenge selection, progress, active-day counting, ranking, rollover and
the colony's day boundaries all resolve through this one class, because when they each computed it
separately a single divergence silently moved Sunday-night matches into the wrong week. Instants stay
stored in UTC; only their calendar interpretation uses the configured zone.

**`scoring/DefaultScoringRuleset`** — the whole barème, in one class: match damage per mode and
outcome, daily diminishing returns, challenge damage, regularity and team bonuses, boss weight
classes and the run's boss ladder. It is **unversioned on purpose** — `V27` dropped the
per-encounter `ruleset_version`, which had made a week's barème depend on when its encounter happened
to be created (a past week with no encounter fell back on version 1, so two adjacent weeks could
score under different rules for no observable reason). One consequence to keep in mind: **changing a
number here changes every week that is recalculated afterwards.** Finalized weeks keep their stored
snapshots until something asks for a recalculation.

**`colony/DefaultColonyRuleset`** — the colony's mirror of the above, and just as central. Food
divisor, efficiency curve, presence threshold, food window, gap-closing rate, morale table, materials
per boss and per challenge, tier step and tier names. Challenge materials derive from
`ScoringRuleset#challengeDamage`, so the colony cannot drift from the ranking, and no anti-farming
rule lives here: the scoring ruleset's daily diminishing returns already applied upstream.

**`colony/ColonyReplayEngine`** — pure arithmetic, and its **order of operations is normative**:
rollover settlement first (challenge materials, boss materials and morale, then efficiency), then the
night (harvest in, day-8 harvest out, population moves a share of its gap). `ColonyReplayService`
**never mutates state incrementally** — it replays the run from day one and rewrites its
`ColonyDailySnapshot`s, which is what makes the daily tick, the post-sync hook and
`POST /api/admin/colony/recompute` safe in any order and safe to fire twice.

**`run/RunService`** — a run is exactly ten weekly rollovers, replacing the Valorant act, which had no
regular duration and so made two campaigns incomparable. `currentRun()` for readers,
`ensureRunFor(weekStart)` for the rollover, which loops so a rollover firing after a long outage
catches several runs up at once and keeps them contiguous — contiguity being what makes a run's 71st
day (its settlement day, carrying its score) also the first day of the next.

**`boss/BossCalibrationService`** — sizes the fight from the **median** per-player output of recent
finalized weeks. A mean would let one marathon week raise the bar for everyone. The category weights
(65 / 85 / 105 % of that reference) sit below or barely above it deliberately: at 100 % the squad is
asked to repeat itself exactly, and ordinary week-to-week noise decides the fight instead of effort.

**`boss/BossChronologyService`** — walks a global chronological timeline of the week's valued matches
against the boss's hit points, to decide whether it fell and which match dealt the finishing blow.
The regularity bonus is deliberately excluded from that timeline: it is a weekly award, not a hit.

**`challenge/calculator/`** — progress rules are pluggable calculators (`CountMatches`,
`DistinctCount`, `MaxGroup`, `MaxStreak`, `Ratio`, `Sum`) dispatched by
`ChallengeProgressCalculatorRegistry`. A challenge in the catalogue is data — a JSONB condition
payload naming a metric, an operator, a target and an optional game-mode filter — not code. Adding a
challenge is an `INSERT`; adding a *kind* of challenge is a new calculator.

**`match/model/GameMode`** — each constant owns the Henrik/Riot identifiers it answers to, matched in
normalized form, because Riot exposes the same mode under different spellings depending on the field.
Two flags carry real weight: `roundBased` (whether ACS/ADR averages mean anything) and
`importEligible` (whether synchronization stores the mode at all). `importEligible` is deliberately
**not** configurable: the set of imported modes defines what "this season is fully synchronized"
means, and nothing records which set was in force, so widening it later would leave permanent
invisible holes in every already-complete season. `OTHER` is imported despite not being tracked — an
unrecognized queue is precisely the case where eligibility cannot be decided yet, and the raw slug is
kept in `valorant_match.queue_id` so a later reclassification is a data migration rather than a
re-import.

### The import pipeline

`StandardSynchronizationScheduler` → `SynchronizationCommandService` → per player
`PlayerSynchronizationService` → `SeasonMatchHistoryWalker` → `MatchImportService`.

Three invariants hold this together:

- **No surrounding transaction.** External API calls stay outside the database transaction, each
  player's outcome is committed independently, and the per-season completion flag depends on each
  step committing on its own. `NonTransactionalGuard` enforces this at
  `PlayerSynchronizationService#synchronize`. Making the pipeline transactional would defer every
  commit to the end of the batch and let one rollback erase the state that says a season is still
  being caught up.
- **Completion must be proved.** A season is marked complete only once the walk reached its oldest
  match — by crossing into an older season or exhausting the history. Until then the next run
  re-walks the season in full rather than stopping at the first already-stored match. Stop conditions
  are evaluated on the raw Henrik page, never on the subset actually imported: a page holding nothing
  but ignored modes proves nothing about the history behind it.
- **Importing is only half the job.** Challenge progress, the ranking and the colony are *derived*;
  they stay stale until rebuilt. Every execution that imported something ends with a challenge
  recalculation and a colony replay, which is what keeps the live view current between two scheduled
  runs.

`StaleSynchronizationReconciler` cleans up runs left `RUNNING` by a crashed process.
`PlayerSeasonSynchronization` carries the resumable checkpoint.

### The Henrik client

`henrik/` wraps a single external dependency behind three interfaces — `HenrikAccountClient`,
`HenrikMatchClient`, `HenrikMmrClient` — each with a `Default*` implementation. The supporting
infrastructure is where the interesting behaviour lives:

- `HenrikRequestLimiter` — client-side rate limiting, kept below the provider's ceiling by a
  configurable safety margin.
- `HenrikRetryStrategy` — two separate budgets. A genuine transient failure (timeout, 5xx) gets
  `HENRIK_API_MAX_ATTEMPTS` (4). A 429 gets `HENRIK_API_RATE_LIMIT_MAX_ATTEMPTS` (25), because being
  throttled during a long history walk is expected, not a failure, and giving up there would abort an
  otherwise healthy synchronization only because it ran long.
- `henrik/exception/` — a dedicated hierarchy separating rate-limit, timeout, not-found,
  service-unavailable and generic failures, so callers can react to each differently.

### Weekly rollover

`WeeklyRolloverScheduler` (Mondays 00:05) → `DefaultWeeklyRolloverService`, which runs the whole
rollover **in one transaction**: refresh the closing week's progress, finalize its challenges and
score snapshots, settle its boss encounter, ensure the run covering the opening week, then draw the
new boss and challenge pack. A failure opening the new week also rolls back the closing of the old
one — the alternative, a half-turned week, has no sane repair path.

`WeeklyLifecycleCoordinator` exists purely to group the boss-specific collaborators so the rollover
service's constructor stays within the codebase's parameter-count limit.

`DefaultWeeklyBossSelectionService` never replaces an existing selection, draws deterministically from
`BossCatalogEntry` within the weight class the run's week index schedules
(`ScoringRuleset#bossCategoryForRunWeek`), cycles through the class before repeating an entry, and can
re-size a still-open encounter once its predecessor has settled. The catalogue holds 6 minor, 10
standard and 6 elite entries — enough for a run of ten weeks never to fight the same boss twice.

### Scheduling

Three cron jobs, configured in `shared/config/SchedulingConfig.java` / `ApplicationProperties.java`,
individually toggleable:

| Scheduler | Default cron | Zone |
| --- | --- | --- |
| `StandardSynchronizationScheduler` | `0 0/30 * * * *` | `SCHEDULING_ZONE` |
| `WeeklyRolloverScheduler` | `0 5 0 * * MON` | `WEEK_ROLLOVER_ZONE` |
| `ColonyDailyTickScheduler` | `0 15 0 * * *` | `WEEK_ROLLOVER_ZONE` |

The colony tick fires ten minutes after the rollover so a Monday's tick sees the week it just
finalized. It is only a safety net — every synchronization replays the colony too — and harmless to
fire twice, the replay being idempotent.

Both zones **must** stay on `app.scheduling.week-rollover-zone`, which `WeekCalendar` reads: it
decides which day a match counts towards and where a week starts, so a job firing in another zone
would settle a week, or replay a colony day, on boundaries that are not the ones producing its data.

## API surface

The live contract is Swagger UI. What follows is the map.

### Public — no credential

```http
GET /api/players
GET /api/players/{playerId}?seasonId&gameMode&weekStart
GET /api/players/{playerId}/progression
GET /api/players/{playerId}/matches?page&size&seasonId&map&agent&result&gameMode
GET /api/seasons
GET /api/challenges/current
GET /api/rankings/current
GET /api/rankings/history?page&size
GET /api/boss/current
GET /api/boss/history?page&size
GET /api/colony
GET /api/colony/trajectory
GET /api/colony/history
```

### Administrative — `X-Admin-Key` required

```http
GET    /api/admin/session                                   # key probe, changes nothing

POST   /api/admin/synchronizations                          # all active players, async
POST   /api/admin/players/{playerId}/synchronizations       # one player, async
GET    /api/admin/synchronizations/latest
GET    /api/admin/synchronizations?page&size
GET    /api/admin/synchronizations/{synchronizationId}

POST   /api/admin/weeks/current/selection                   # force this week's boss + challenge draw
POST   /api/admin/challenges/progress/recalculation
POST   /api/admin/rankings/recalculation
POST   /api/admin/colony/recompute                          # replays the run in progress

GET    /api/admin/players                                   # includes archived
POST   /api/admin/players
PUT    /api/admin/players/{playerId}
PATCH  /api/admin/players/{playerId}/status
DELETE /api/admin/players/{playerId}

PATCH  /api/admin/matches/{matchId}/game-mode               # correct a misclassified match
POST   /api/admin/maintenance/campaign-reset                # irreversible
```

The three recalculation routes are non-destructive by construction: challenge progress, the ranking
and the colony are derived from stored matches, so re-running them only reproduces them.

### Conventions

- Paginated collections share one `PageResponse<T>` envelope (`shared/dto/`), and every paged
  endpoint validates its request through `shared/util/PaginationGuard.assertValidPageRequest` —
  a negative index or a size outside `1..100` is a 400, not a 500 or a full-table fetch.
- Failures share one problem-details shape, `ApiErrorResponse` (`type`, `title`, `status`, `code`,
  `detail`, `instance`, `timestamp`, `errors`), produced by `GlobalExceptionHandler`. Domain code
  throws `ResourceNotFoundException`, `InvalidRequestException` or `ConflictException`; the handler
  owns the HTTP mapping.
- `spring.jackson.default-property-inclusion=non_null` — absent fields are omitted, not null.
- Timestamps are serialized in UTC.
- Synchronization commands return immediately and run on the executor declared in `AsyncConfig`;
  progress is observed through `GET /api/admin/synchronizations/latest`.

## Security

`SecurityConfig` builds a stateless chain: CSRF disabled, no sessions, CORS restricted to
`app.frontend-origin`, allowed headers limited to `Content-Type`, `Authorization` and `X-Admin-Key`.

Administrative access is split across two steps that must both agree. `AdminApiKeyFilter`, inserted
before `UsernamePasswordAuthenticationFilter`, compares `X-Admin-Key` against `app.admin-api-key` in
**constant time** and grants `ROLE_ADMIN` on success; `SecurityConfig` then *requires* that authority
on `/api/admin/**`. The filter decides **why** a key is refused — 401 missing header, 403 wrong key,
429 locked out — and the authorization rules decide **whether** the request proceeds, so a request
that somehow reaches an administrative handler without passing the filter is still denied. Everything
not explicitly permitted — `GET /api/**`, actuator health/info, Swagger when enabled — is denied.

Two details are load-bearing, and both are covered by `AdminApiSecurityIntegrationTest`:

- The admin rule is declared **before** the public `GET /api/**` rule. Spring Security applies the
  first matching rule, so the reverse order would leave every administrative read endpoint open.
- The filter recognises administrative routes with a `PathPatternRequestMatcher`, sharing
  `AdminApiKeyFilter.ADMIN_PATH_PATTERN` with `SecurityConfig` — *not* with a raw
  `getRequestURI().startsWith("/api/admin")`. The request URI is still percent-encoded while Spring
  matches handlers on the decoded path, so `/api/%61dmin/...` reaches the administrative controller
  while failing a raw prefix test.

`AdminAuthRateLimiter` throttles repeated invalid keys per remote address in memory: after
`app.admin-rate-limit.max-failures` failures the address is locked out with HTTP 429 for
`app.admin-rate-limit.lockout-duration`. A valid key clears the counter. Because the keys are
unauthenticated remote addresses, expired windows are swept once the map passes a threshold —
otherwise an address that fails once and never returns would be tracked forever.

That per-address key needs `server.forward-headers-strategy=framework`
(`FORWARD_HEADERS_STRATEGY`) behind a TLS-terminating proxy: without it every request reports the
proxy's address and the lockout collapses into a global one. Use `none` only when the application is
exposed directly, where a caller could forge the header itself.

## Persistence

Flyway owns the schema (`src/main/resources/db/migration/`, `V<N>__<description>.sql`, sequential,
currently through `V35`). Hibernate runs `ddl-auto=validate` and will refuse to start against a schema
that does not match the entities.

**Never edit an applied migration — only append a new one.** `FlywayMigrationIntegrationTest` runs the
full chain against a real Postgres container on every `verify`.

The [`flyway-migration` skill](../.claude/skills/flyway-migration) scaffolds a correctly numbered file
if you use Claude Code; otherwise copy the naming from the latest one.

Tables, grouped by what they hold:

| Group | Tables |
| --- | --- |
| Roster and identity | `player`, `season` |
| Match history | `valorant_match`, `player_match` |
| Challenges | `challenge` (catalogue), `weekly_challenge` (the week's pack), `player_challenge_progress` |
| Boss | `boss_catalog_entry`, `weekly_boss_encounter` |
| Ranking | `weekly_player_score` |
| Campaign | `run`, `colony_daily_snapshot` |
| Import bookkeeping | `synchronization`, `synchronization_player_result`, `player_season_synchronization` |

Three shapes are worth noting:

- `weekly_boss_encounter` does **not** store total damage dealt — it is derived from
  `weekly_player_score`, which already tracks it per player, so there is one source of truth.
- `run` freezes its `roster_size` at opening. The backoffice can activate, deactivate or archive a
  player at any time, and reading the roster live would let an archive rewrite the history of a run
  already played.
- `player` uses a lifecycle status rather than deletion: `ACTIVE`, `INACTIVE` (still tracked and still
  completes challenges individually, but contributes no boss damage and takes no ranking slot) and
  `ARCHIVED` (absent from public listings but still resolvable, because a finalized week may credit it
  with the kill that ended a boss). Archiving is what a deletion becomes once a player has fought a
  boss, and it is reversible — which is what makes it acceptable.

`colony_daily_snapshot` is deliberately rich: it stores each day's inputs and outputs, not just the
resulting population, so the ruleset can be recalibrated afterwards without asking Henrik for anything
again.

Local reset, destroys everything:

```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO valorant;
GRANT ALL ON SCHEMA public TO public;
```

## Configuration

`.env` is loaded at startup by `spring-dotenv`; real environment variables take precedence, which is
how the container image is configured. `.env.example` is the annotated reference — copy it rather than
this table when setting up.

### Required

| Variable | Notes |
| --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Default to `jdbc:postgresql://localhost:5432/valo_quests`, `valorant`, `valorant` — matching `docker-compose.yml` |
| `HENRIK_API_KEY` | No default. Synchronization fails without it |
| `ADMIN_API_KEY` | No default. Protects `/api/admin/**`; use ≥ 32 random characters |

### Everything else

| Variable | Default | Controls |
| --- | --- | --- |
| `FRONTEND_ORIGIN` | `http://localhost:4200` | The single origin allowed by CORS |
| `API_DOCS_ENABLED` | `false` (`.env.example` sets `true`) | Serves `/api-docs` and `/swagger-ui.html`. Leave off anywhere reachable |
| `FORWARD_HEADERS_STRATEGY` | `framework` | How `X-Forwarded-*` is handled. Keep `framework` behind a proxy, `none` when exposed directly |
| `ADMIN_RATE_LIMIT_MAX_FAILURES` | `5` | Invalid-key attempts before lockout |
| `ADMIN_RATE_LIMIT_LOCKOUT_DURATION` | `PT1M` | How long an address stays locked out |
| `HENRIK_API_BASE_URL` | `https://api.henrikdev.xyz` | Henrik endpoint |
| `HENRIK_API_REGION` / `HENRIK_API_PLATFORM` | `eu` / `pc` | Shared by every tracked player |
| `HENRIK_API_CONNECT_TIMEOUT` / `HENRIK_API_READ_TIMEOUT` | `PT5S` / `PT20S` | HTTP timeouts |
| `HENRIK_API_MAX_ATTEMPTS` | `4` | Attempts on a transient failure, initial request included |
| `HENRIK_API_RETRY_DELAY` | `PT60S` | Delay before retrying; a larger `Retry-After` wins |
| `HENRIK_API_RATE_LIMIT_MAX_ATTEMPTS` | `25` | Attempts on HTTP 429 — a much larger budget on purpose |
| `HENRIK_API_REQUESTS_PER_MINUTE` | `28` | Client-side ceiling, below the provider's |
| `HENRIK_API_RATE_LIMIT_SAFETY_MARGIN` | `PT0.1S` | Extra spacing between two requests |
| `STANDARD_SYNC_ENABLED` / `STANDARD_SYNC_CRON` | `true` / `0 0/30 * * * *` | Automatic import, which also replays the colony |
| `SCHEDULING_ZONE` | `Europe/Paris` | Zone of the import cron |
| `WEEK_ROLLOVER_ENABLED` / `WEEK_ROLLOVER_CRON` | `true` / `0 5 0 * * MON` | Automatic rollover |
| `COLONY_TICK_ENABLED` / `COLONY_TICK_CRON` | `true` / `0 15 0 * * *` | Daily colony replay, a safety net behind the sync |
| `WEEK_ROLLOVER_ZONE` | `Europe/Paris` | **Zone of every weekly and daily calculation** *and* of the rollover and colony crons. The three must stay on this single value — a mismatch moves end-of-week matches into the wrong week |

`PT5S`, `PT1M`, `PT0.1S` are ISO-8601 durations.

Disabling the schedulers (`STANDARD_SYNC_ENABLED=false`, `WEEK_ROLLOVER_ENABLED=false`,
`COLONY_TICK_ENABLED=false`) and driving everything from `/api/admin/**` is the normal way to work on
the weekly loop locally.

## Testing

| Kind | Runs against | Notes |
| --- | --- | --- |
| Unit (`*Test.java`) | In-memory H2, `ddl-auto=create-drop`, Flyway off, schedulers and Henrik stubbed | Henrik HTTP mocked with OkHttp `MockWebServer` |
| Integration (`*IntegrationTest.java`) | A shared `postgres:17-alpine` Testcontainer running the real migrations | Extend `integration.PostgreSqlIntegrationTest`; tagged `@Tag("integration")` and `@Testcontainers(disabledWithoutDocker = true)` |

Without Docker, integration tests are **skipped, not failed** — so a green `./mvnw verify` on a
machine without Docker proves less than a green CI run. The suites worth reading first are
`SynchronizationPipelineIntegrationTest`, `WeeklyLifecycleIntegrationTest`,
`ColonyRunIntegrationTest` and `AdminApiSecurityIntegrationTest`.

## Quality gates

All three run inside `./mvnw verify`, bound to Maven phases. They fail the build; none of them warn.

- **Checkstyle** (`validate`) — `config/checkstyle/checkstyle.xml`, applied to main *and* test
  sources. 120-char lines, sorted imports, Javadoc required on public types and methods,
  `NestedIfDepth` ≤ 2, no committed `TODO`. Suppressions
  (`config/checkstyle/suppressions.xml`) must be narrow and justified — a rule that needs a blanket
  suppression belongs in the config instead.
- **SpotBugs** (`verify`) — `effort=Max`, `threshold=Low`. Justified exceptions use
  `@SuppressFBWarnings` with a reason, scoped as tightly as possible.
- **JaCoCo** (`verify`) — bundle-level: **line ≥ 90 %**, **branch ≥ 70 %**. A ratchet floor against
  erosion, not a target. Raise it when coverage genuinely improves; never lower it to make a build
  pass.

## Container image

`Dockerfile` is a two-stage build: `eclipse-temurin:25-jdk` packages the jar (quality gates already
ran in CI, so this stage uses `-DskipTests`), `eclipse-temurin:25-jre` runs it as a non-root user and
exposes `8080`.

```bash
docker build -t valo-quests-backend backend/    # from the repo root
```

The image needs the same environment variables as a local run, and `DB_URL` must point at a reachable
Postgres — `docker-compose.yml`'s `postgres` service exists for local `spring-boot:run` and is not
consumed by this image. No `HEALTHCHECK` is declared; orchestrators are expected to probe
`/actuator/health` themselves.

## Contributing safely

Things that will bite you if you skip them:

- **Never modify an applied Flyway migration.** Append.
- **Moving a number in `DefaultScoringRuleset` or `DefaultColonyRuleset` is a balance change, not a
  refactor.** The barème is unversioned, so it applies to every week recalculated afterwards. Change
  one deliberately, with its Javadoc updated to say why, and expect the frontend constants mirroring
  it (`rules.constants.ts`, `colony-view.ts`) to need the same move.
- **Never wrap the synchronization pipeline in a transaction.** `NonTransactionalGuard` will stop you
  at runtime; the reason is above.
- **Never make the colony mutate incrementally.** Replay is what makes every command idempotent, and
  `ColonyReplayEngine`'s order of operations is normative.
- **Keep the week-related zones on one property.** `WEEK_ROLLOVER_ZONE` governs the calendar, the
  rollover cron and the colony tick.
- Run `./mvnw verify` before pushing. Checkstyle in particular fails on things an IDE will not warn
  about — missing Javadoc on a public method, unsorted imports.

## Troubleshooting

| Symptom | Cause / fix |
| --- | --- |
| Startup fails on a database connection | Postgres is down or `.env` disagrees with it. `docker compose ps` |
| Startup fails on schema validation | A migration is missing or an entity drifted. Check Flyway's log, add a migration |
| `/swagger-ui.html` returns 404 | `API_DOCS_ENABLED` is not `true` |
| `/api/admin/**` returns 401 | `X-Admin-Key` missing or wrong |
| `/api/admin/**` returns 429 | Rate-limit lockout after repeated invalid keys. Wait out `ADMIN_RATE_LIMIT_LOCKOUT_DURATION` |
| No player data after start | Nothing synchronized yet, or `HENRIK_API_KEY` is missing/invalid. Trigger `POST /api/admin/synchronizations` and watch `.../synchronizations/latest` |
| No boss and no challenges | The week was never opened. `POST /api/admin/weeks/current/selection` |
| A synchronization ends `PARTIAL` | Per-player failures are recorded, not fatal. Inspect `GET /api/admin/synchronizations/{id}` |
| Ranking looks stale after an import | Recalculation is what refreshes it. `POST /api/admin/challenges/progress/recalculation`, then `.../rankings/recalculation` |
| Colony gauges look stale or wrong | `POST /api/admin/colony/recompute` — it replays the run from its inputs |
| `./mvnw verify` silently skips tests | Docker is unavailable — integration tests are skipped by design |
| CORS error from the frontend | `FRONTEND_ORIGIN` does not match where the frontend actually runs |
| Admin lockout hits every caller at once behind a proxy | `FORWARD_HEADERS_STRATEGY` is `none`; it should be `framework` |

## Related documents

- [Root README](../README.md) — the product, and the from-scratch setup walkthrough.
- [frontend/README.md](../frontend/README.md) — the consumer of this API.

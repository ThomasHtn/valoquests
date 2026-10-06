# ValoQuests — Backend

Spring Boot API that imports Valorant matches from the HenrikDev API, replays them into the campaign
described in [`docs/GAMEPLAY.md`](../docs/GAMEPLAY.md), and exposes the result to the
[frontend](../frontend/README.md).

|           |                                                                                      |
| --------- | ------------------------------------------------------------------------------------ |
| Language  | Java 25                                                                              |
| Framework | Spring Boot 4.0.6 (Web, Security, WebClient, Data JPA, Validation, Flyway, Actuator) |
| Database  | PostgreSQL 17, schema owned by Flyway                                                |
| API docs  | springdoc OpenAPI, Swagger UI at `/swagger-ui.html`                                  |
| Tests     | JUnit 5, AssertJ, Mockito, Testcontainers, MockWebServer, ArchUnit                   |
| Gates     | Spotless, Checkstyle, SpotBugs, JaCoCo (90% line / 70% branch)                       |

## Getting started

Requirements: JDK 25, Docker (for PostgreSQL and the integration tests), a HenrikDev API key.

```bash
docker compose up -d          # PostgreSQL 17 on :5432
cp .env.example .env          # then set HENRIK_API_KEY and ADMIN_API_KEY
./mvnw spring-boot:run        # API on :8080
```

Flyway migrates the schema on startup. Swagger UI is available at
`http://localhost:8080/swagger-ui.html` while `API_DOCS_ENABLED=true`.

## Commands

```bash
./mvnw verify                                            # the full gate, what CI runs
./mvnw test                                              # unit + integration tests
./mvnw test -Dtest=CampaignReplayEngineTest              # one class
./mvnw test -Dtest=CampaignReplayEngineTest#methodName   # one method
./mvnw spotless:apply                                    # fix imports, whitespace and final newlines
./mvnw checkstyle:check                                  # style only (also bound to `validate`)
./mvnw spotbugs:check                                    # bytecode analysis only
```

`verify` is strict on purpose: Spotless fails on any fixable formatting drift, Checkstyle on any violation (including `TodoComment`), SpotBugs
runs at max effort and low threshold, and JaCoCo enforces its coverage floors over the whole bundle.
The floors are a ratchet: raise them when coverage improves, never lower them to make a build pass.

## Architecture

Packaged by feature under `io.github.thomashtn.valoquests`:

```
henrik  shared  player  match  scoring  challenge  ranking  campaign  synchronization  profile  week  maintenance  roster
```

Each feature owns its `entity`/`model`, `repository`, `service`, `controller`, `dto` and `exception`
subpackages. Cross-cutting configuration lives in `shared/config`, the calendar in `shared/time`,
`MatchHistoryLock` in `shared/concurrency`, guards and error plumbing in `shared/util` and
`shared/exception`.

### Package map

The packages form a one-way graph. Each one sits on a tier and may only use packages of a lower
tier: never one above it, never one beside it. Read the table bottom-up as the order the domain is
built in: a Henrik payload becomes a player's match, the match is priced, prices feed challenges,
challenges feed the ranking, all of it is replayed into the campaign, and the jobs on top drive it.

| Tier | Package           | Owns                                                                                                                                                                                           | Why it sits there                                                                                |
| ---- | ----------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| 0    | `henrik`          | HenrikDev HTTP clients, rate limiter, retries, payload DTOs                                                                                                                                    | A self-contained transport client, it knows nothing of the app                                   |
| 1    | `shared`          | Config, security, error rendering, `WeekCalendar`, `MatchHistoryLock`, pagination and transaction guards                                                                                       | Infrastructure every feature uses; it only knows `henrik` to render provider failures as 429/502 |
| 2    | `player`          | The tracked player: entity, status, competitive tier, repository, Riot account resolution                                                                                                      | Every match, score and roster row points at a player                                             |
| 3    | `match`           | Stored matches and seasons, Henrik import, eligibility and outcome rules, game-mode correction, seasons route                                                                                  | Raw match facts, before anyone prices them                                                       |
| 4    | `scoring`         | What a match and a challenge are worth (`ScoringRuleset`, `DailyOutputReader`) and the vocabulary it prices: `ChallengeCadence`, `ChallengeTier`, `ChallengeCalibration`, `CampaignDifficulty` | Prices matches, and challenges need those prices                                                 |
| 5    | `challenge`       | Catalogue, daily and weekly draws, progress calculators, recalculation, challenge routes                                                                                                       | Measures matches against the catalogue and shows the reward scoring sets                         |
| 6    | `ranking`         | Weekly and daily rankings, titles, champion                                                                                                                                                    | Ranks players on priced matches and validated challenges                                         |
| 7    | `campaign`        | Campaign lifecycle, replay engine, base/week/day tables, daily tick                                                                                                                            | Replays everything below into the base, titles included                                          |
| 8    | `synchronization` | Henrik history walk, execution records, sync and purge schedulers                                                                                                                              | Imports matches, then reruns challenges and the replay                                           |
| 8    | `profile`         | Player and match screens: roster list, profile statistics, progression, valued match history, the squad's matches of the day                                                                   | Read side joining matches, prices and the shown campaign's roster                                |
| 9    | `week`            | Weekly rollover (finalize, open), missed-schedule catch-up                                                                                                                                     | Synchronizes, then closes and opens weeks across every feature                                   |
| 9    | `maintenance`     | Campaign reset                                                                                                                                                                                 | Admin job wiping campaign state under the history lock                                           |
| 9    | `roster`          | Roster administration: add, edit, retire, delete players                                                                                                                                       | Deleting a player wipes what every feature derived from them                                     |

The root package only holds `ValoQuestsApplication`.

**How it is enforced.** `ArchitectureTest` (ArchUnit, run by `./mvnw verify` with the unit tests)
fails the build when a package uses one of a higher or equal tier, when two top-level packages form a
cycle, when a controller uses a repository directly, or when a new top-level package has not been
given a tier in its `TIERS` list. A new package is placed there first, with a line in the table above.

**When a lower package needs something from a higher one**, move the class to the package that owns
its data or its job first. A small interface in the lower package, implemented by the higher one, is
kept for the few genuine cases where the lower package must ask or notify the one above:

| Port (lower package)                            | Implemented in | Why                                                                         |
| ----------------------------------------------- | -------------- | --------------------------------------------------------------------------- |
| `challenge.service.ChallengeCalibrationSource`  | `campaign`     | Challenge targets and rewards depend on the campaign covering the week      |
| `challenge.service.CurrentWeekProgressListener` | `ranking`      | Rebuilt challenge progress must rebuild the ranking in the same transaction |
| `ranking.service.WeekCoverageSource`            | `campaign`     | A champion is only crowned on a week a campaign covered                     |

One coupling stays invisible to the test: the JPQL of `PlayerMatchRepository.findSquadHistory`
(`match`) names the `CampaignPlayer` entity to filter the squad on the shown campaign's roster. Only
`profile` calls it.

### The four loops

Everything converges on a single replay.

| Loop            | Trigger                                              | What it does                                                                                                                                                                                          |
| --------------- | ---------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Synchronization | every 5 min (`STANDARD_SYNC_CRON`)                   | `SynchronizationCommandService` → `PlayerSynchronizationService` → `SeasonMatchHistoryWalker` walks the current and previous seasons, checkpointing per season so an interrupted walk resumes         |
| Daily tick      | 00:10 (`CampaignDailyTickScheduler`)                 | `DailyTickService` draws the day's challenge, rebuilds progress and ranking, starts a campaign whose first Monday has come, then replays it                                                           |
| Weekly rollover | Monday 02:05 (`WeeklyRolloverScheduler`)             | Imports the last Sunday matches, then `WeeklyRolloverService` finalizes the previous week (`WeekFinalizer`), opens the new one and closes a finished campaign (`WeekOpener`), in a single transaction |
| Campaign replay | every one of the above                               | `CampaignReplayService` → `CampaignReplayEngine` rebuilds the campaign from day one                                                                                                                   |
| History purge   | daily 03:30 (`SynchronizationHistoryPurgeScheduler`) | `SynchronizationHistoryPurger` deletes the passes that imported nothing once they are older than `SYNC_QUIET_HISTORY_RETENTION` (7 days)                                                              |

### Invariants

- **Nothing is ever incremented.** The campaign is rebuilt from its matches and challenges on every
  synchronization, every nightly tick and every admin action. `CampaignReplayEngine` is pure: no
  repository, no clock, no entity, so the same inputs always yield the same base.
- **Replay steps are idempotent and order-sensitive** in the way the engine encodes them: the base
  grows, stocks fill, the base eats, Sunday the ship leaves, the rescued arrive after the guardian
  has struck.
- **`WeekCalendar` resolves day and week boundaries in `CALENDAR_ZONE`** (Europe/Paris). The
  schedulers fire in that same zone, or a day would be closed on boundaries that did not produce
  its gains.
- **A closed campaign is frozen** and never replayed again.
- `PlayerSynchronizationService` is deliberately non-transactional, enforced by
  `NonTransactionalGuard`: Henrik calls must stay outside a transaction or the per-season completion
  flags stop being honest.
- **One job at a time writes the match history.** `MatchHistoryLock` serializes synchronizations,
  the weekly rollover, the daily tick, the challenge redraw, the ranking recalculation, the player
  deletion and the campaign reset; the app runs as a single instance.

### Challenges as data

Challenge progress is computed by a registry of calculators (`challenge/calculator`) selected by
`ProgressMode` (sum, count matches, distinct count, ratio, max streak, max group, all).
Definitions are parsed from the catalogue by `JacksonChallengeDefinitionParser`, which reads the
amateur or pro grid the campaign's `CampaignDifficulty` selects; nothing is derived from match history.
Adding a challenge shape usually means a new calculator plus catalogue rows, not new controller code.

## API surface

`GET /api/**` is public. Everything under `/api/admin/**` requires the `X-Admin-Key` header.

| Public                                      | Admin                                                                     |
| ------------------------------------------- | ------------------------------------------------------------------------- |
| `/api/campaign`                             | `/api/admin/campaigns`                                                    |
| `/api/challenges`                           | `/api/admin/challenges`                                                   |
| `/api/players`, `/api/players/{id}/matches` | `/api/admin/players`, `/api/admin/matches`                                |
| `/api/rankings`                             | `/api/admin/rankings`, `/api/admin/weeks`                                 |
| `/api/seasons`, `/api/matches`              | `/api/admin/session`, `/api/admin/maintenance`                            |
| `/api/synchronization`                      | `/api/admin/synchronizations`, `/api/admin/players/{id}/synchronizations` |

`SecurityConfig` is stateless and denies by default; the admin rule must stay ahead of the public GET
rule. `AdminApiKeyFilter` applies a per-remote-address lockout through `AdminAuthRateLimiter`. Behind a
reverse proxy, keep `FORWARD_HEADERS_STRATEGY=framework`, otherwise every caller looks like the proxy
and one attacker locks out everyone.

Errors are thrown as `ResourceNotFoundException` / `InvalidRequestException` / `ConflictException` and
rendered by `GlobalExceptionHandler` into `ApiErrorResponse`. Controllers never build an error
`ResponseEntity`. Every paged endpoint goes through `PaginationGuard.assertValidPageRequest`, capped
at 100 items.

## Persistence

Flyway migrations in `src/main/resources/db/migration` are the schema source of truth, with
`ddl-auto=validate` in production. Never edit an applied migration, add `V<n+1>__*.sql`. JPA runs with
`open-in-view=false` and batch fetching, so a query fetches what it needs explicitly.

## Testing

- **Unit tests** run against in-memory H2 with `ddl-auto=create-drop` and Flyway disabled
  (`src/test/resources/application.properties`).
- **Integration tests** extend `PostgreSqlIntegrationTest` (`@Tag("integration")`), which starts one
  shared PostgreSQL 17 Testcontainer with Flyway on and Hibernate validating the migrated schema.
  Docker must be running, and schema changes are only truly verified there.

Collaborators are built by hand rather than mocked when they are pure. `@DisplayName` is written as a
behaviour sentence, not as a restatement of the method name.

## Configuration

Everything is env-driven through `.env` (loaded as a `.properties` file, so never quote a value) and
typed properties (`ApplicationProperties`, `HenrikApiProperties`). [`.env.example`](.env.example)
documents every knob and the reasoning behind the non-obvious ones. The ones worth knowing before a first run:

| Variable                         | Why it matters                                                    |
| -------------------------------- | ----------------------------------------------------------------- |
| `HENRIK_API_KEY`                 | Required. Without it no match is ever imported                    |
| `ADMIN_API_KEY`                  | Guards `/api/admin/**`. Use a long random secret                  |
| `HENRIK_API_REQUESTS_PER_MINUTE` | Kept under the provider limit; a long history walk depends on it  |
| `CALENDAR_ZONE`                  | The one zone `WeekCalendar` and every scheduled job share         |
| `API_DOCS_ENABLED`               | Leave off anywhere reachable: the document maps every admin route |

Add new settings to `.env.example` too.

### Upgrade notes

- `WEEK_ROLLOVER_ZONE` and `SCHEDULING_ZONE` are replaced by `CALENDAR_ZONE`; the old names are
  ignored, so rename them in an existing `.env`.

## Conventions

- DTOs are `record`s annotated `@Schema`; controllers carry springdoc `@Tag`/`@Operation`/`@ApiResponse`.
- A service a controller calls is an interface plus a `Default*` implementation; internal
  collaborators stay plain classes. Constructor injection only.
- Spotless keeps imports ordered and used, and whitespace clean; `./mvnw spotless:apply` fixes it.
- Checkstyle enforces 4-space indent, no star imports, Javadoc on types and methods, bounded parameter
  counts, no TODO comments. Comments explain intent and constraints, not mechanics.

## Docker

`Dockerfile` builds the executable jar on `eclipse-temurin:25-jdk` and runs it on a JRE image as an
unprivileged user. Quality gates run in CI before an image is ever built, so the build stage only
packages the artifact.

---

Game rules and constants: [`docs/GAMEPLAY.md`](../docs/GAMEPLAY.md) ·
challenge catalogue: [`docs/CHALLENGES.md`](../docs/CHALLENGES.md) ·
product overview: [root README](../README.md).

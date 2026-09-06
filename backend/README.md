# ValoQuests — Backend

Spring Boot API that imports Valorant matches from the HenrikDev API, replays them into the campaign
described in [`docs/GAMEPLAY.md`](../docs/GAMEPLAY.md), and exposes the result to the
[frontend](../frontend/README.md).

| | |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.0.6 (Web, Security, WebClient, Data JPA, Validation, Flyway, Actuator) |
| Database | PostgreSQL 17, schema owned by Flyway |
| API docs | springdoc OpenAPI, Swagger UI at `/swagger-ui.html` |
| Tests | JUnit 5, AssertJ, Mockito, Testcontainers, MockWebServer |
| Gates | Checkstyle, SpotBugs, JaCoCo (90% line / 70% branch) |

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
./mvnw checkstyle:check                                  # style only (also bound to `validate`)
./mvnw spotbugs:check                                    # bytecode analysis only
```

`verify` is strict on purpose: Checkstyle fails on any violation (including `TodoComment`), SpotBugs
runs at max effort and low threshold, and JaCoCo enforces its coverage floors over the whole bundle.
The floors are a ratchet: raise them when coverage improves, never lower them to make a build pass.

## Architecture

Packaged by feature under `io.github.thomashtn.valoquests`:

```
campaign  challenge  henrik  match  player  ranking  scoring  synchronization  week  maintenance  shared
```

Each feature owns its `entity`/`model`, `repository`, `service`, `controller`, `dto` and `exception`
subpackages. Cross-cutting configuration lives in `shared/config`, guards and error plumbing in
`shared/util` and `shared/exception`.

### The four loops

Everything converges on a single replay.

| Loop | Trigger | What it does |
|---|---|---|
| Synchronization | every 30 min (`STANDARD_SYNC_CRON`) | `SynchronizationLaunchService` → `PlayerSynchronizationService` → `SeasonMatchHistoryWalker` walks Henrik season by season, checkpointing per season so an interrupted walk resumes |
| Daily tick | 00:10 (`CampaignDailyTickScheduler`) | Draws the day's challenge, starts a campaign whose first Monday has come, replays it, then closes one past its tenth Sunday, in that order |
| Weekly rollover | Monday 00:05 (`DefaultWeeklyRolloverService`) | Finalizes the previous week and opens the new one in a single transaction |
| Campaign replay | every one of the above | `CampaignReplayService` → `CampaignReplayEngine` rebuilds the campaign from day one |

### Invariants

- **Nothing is ever incremented.** The campaign is rebuilt from its matches and challenges on every
  synchronization, every nightly tick and every admin action. `CampaignReplayEngine` is pure: no
  repository, no clock, no entity, so the same inputs always yield the same base.
- **Replay steps are idempotent and order-sensitive** in the way the engine encodes them: the base
  grows, stocks fill, the base eats, Sunday the ship leaves, the rescued arrive after the guardian
  has struck.
- **`WeekCalendar` resolves day and week boundaries in `WEEK_ROLLOVER_ZONE`** (Europe/Paris). The
  schedulers must fire in that same zone, or a day gets closed on boundaries that did not produce
  its gains.
- **A closed campaign is frozen** and never replayed again.
- `PlayerSynchronizationService` is deliberately non-transactional, enforced by
  `NonTransactionalGuard`: Henrik calls must stay outside a transaction or the per-season completion
  flags stop being honest.

### Challenges as data

Challenge progress is computed by a registry of calculators (`challenge/calculator`) selected by
`ProgressMode` (sum, count matches, distinct count, ratio, max streak, max group, baseline, all).
Definitions are parsed from the catalogue by `JacksonChallengeDefinitionParser`, and their targets are
resolved per squad by `ChallengeTargetResolver` against the calibration from `SquadCalibrationService`.
Adding a challenge shape usually means a new calculator plus catalogue rows, not new controller code.

## API surface

`GET /api/**` is public. Everything under `/api/admin/**` requires the `X-Admin-Key` header.

| Public | Admin |
|---|---|
| `/api/campaign` | `/api/admin/campaigns` |
| `/api/challenges` | `/api/admin/challenges` |
| `/api/players`, `/api/players/{id}/matches` | `/api/admin/players`, `/api/admin/matches` |
| `/api/rankings` | `/api/admin/rankings`, `/api/admin/weeks` |
| `/api/seasons` | `/api/admin/session`, `/api/admin/maintenance` |

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

Everything is env-driven through `.env` (spring-dotenv) and typed properties (`ApplicationProperties`,
`HenrikApiProperties`). [`.env.example`](.env.example) documents every knob and the reasoning behind
the non-obvious ones. The ones worth knowing before a first run:

| Variable | Why it matters |
|---|---|
| `HENRIK_API_KEY` | Required. Without it no match is ever imported |
| `ADMIN_API_KEY` | Guards `/api/admin/**`. Use a long random secret |
| `HENRIK_API_REQUESTS_PER_MINUTE` | Kept under the provider limit; a long history walk depends on it |
| `SCHEDULING_ZONE`, `WEEK_ROLLOVER_ZONE` | Both must stay on the same zone as `WeekCalendar` |
| `API_DOCS_ENABLED` | Leave off anywhere reachable: the document maps every admin route |

Add new settings to `.env.example` too.

## Conventions

- DTOs are `record`s annotated `@Schema`; controllers carry springdoc `@Tag`/`@Operation`/`@ApiResponse`.
- Services are an interface plus a `Default*` implementation, constructor injection only.
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

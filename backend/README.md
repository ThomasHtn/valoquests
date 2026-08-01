# Backend

Java 25 / Spring Boot API for Valorant Tracker. See the [root README](../README.md) for the project overview and the
[frontend README](../frontend/README.md) for the Angular application.

## Documentation

This README covers setup, build and operations. The design is documented in [`docs/`](docs):

| Document                                            | Covers                                                                     |
| --------------------------------------------------- | -------------------------------------------------------------------------- |
| [Architecture](docs/architecture.md)                | Package layout, layering, transactions, configuration, error handling       |
| [Synchronization](docs/synchronization.md)          | The Henrik import pipeline, season scope, idempotency, failure isolation    |
| [Challenge engine](docs/challenge-engine.md)        | Rule format, the seven calculators, weekly selection, progress rules        |
| [API](docs/api.md)                                  | Every route, its parameters, its response shape and its failure modes       |

Project-wide context is in [`../docs/`](../docs): [architecture](../docs/architecture.md),
[domain model](../docs/domain-model.md), [data model](../docs/data-model.md) and the
[decision records](../docs/adr/README.md).

## Architecture at a glance

Code is organized by business feature. Each domain owns its controllers, services, repositories, entities, DTOs and
business models.

```text
io.github.thomashtn.valorant.tracker
├── challenge        Weekly selection, rule parsing and progress calculation
├── henrik           External API clients, mapping, retry and rate limiting
├── match            Seasons, matches, statistics and idempotent imports
├── player           Tracked accounts, profiles and Riot account resolution
├── ranking          Weekly scores, positions and ranking history
├── synchronization  Import orchestration, scheduling and monitoring
├── week             Week calendar and weekly rollover
└── shared           Security, errors, auditing and common web components
```

Thin controllers, explicit transaction boundaries, constructor injection, immutable API DTOs, and database constraints
for every critical invariant. Full details in [`docs/architecture.md`](docs/architecture.md).

Two behaviors surprise readers often enough to be worth stating here:

- **Synchronization is deliberately non-transactional.** Adding `@Transactional` above the walk would look like an
  improvement and would break the guarantee that an interrupted import leaves no permanent hole. See
  [ADR 0005](../docs/adr/0005-non-transactional-synchronization.md).
- **Match history is bounded to the current act.** A player's stored history begins there, so match counts read lower
  than the lifetime totals shown by external trackers such as tracker.gg. Every player result records why its walk
  stopped, so a short history explains itself. See [ADR 0008](../docs/adr/0008-season-scoped-history.md).

## Getting started

### Requirements

- JDK 25
- Docker and Docker Compose, or PostgreSQL 17
- a Henrik API key

The Maven Wrapper downloads Maven on its first execution, so an internet connection is required the first time it runs.

Commands below assume `backend/` as the working directory.

### Configure the environment

```bash
cp .env.example .env
```

Set at least:

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/valorant_tracker
DB_USERNAME=valorant
DB_PASSWORD=change-me
ADMIN_API_KEY=replace-with-a-long-random-secret
HENRIK_API_KEY=your-henrik-api-key
```

### Start PostgreSQL

```bash
docker compose up -d
docker compose ps
```

### Run the backend

```bash
./mvnw spring-boot:run
```

Useful URLs:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI document: `http://localhost:8080/api-docs`
- Health check: `http://localhost:8080/actuator/health`

Use Swagger's **Authorize** action to provide `X-Admin-Key` for administrative routes.

## Build and tests

Run the complete quality gate:

```bash
./mvnw clean verify
```

This command runs Checkstyle, unit and integration tests, the JaCoCo report and its coverage gate (90 % line, 70 %
branch) and SpotBugs. Docker must be available for the integration suite, which starts an isolated PostgreSQL 17
container; without Docker those tests are skipped rather than failed, so a green build without Docker proves less than
it appears to.

Run only the fast test suite:

```bash
./mvnw test -DexcludedGroups=integration
```

Run only integration tests:

```bash
./mvnw test -Dgroups=integration
```

Run one test class:

```bash
./mvnw -Dtest=PlayerSynchronizationServiceTest test
```

The JaCoCo XML report is generated at `target/site/jacoco/jacoco.xml` and is ready for SonarQube:

```bash
./mvnw clean verify sonar:sonar \
  -Dsonar.projectKey=valorant-tracker \
  -Dsonar.host.url="$SONAR_HOST_URL" \
  -Dsonar.token="$SONAR_TOKEN"
```

## Database migrations

Flyway is the only schema-management mechanism. Hibernate uses `ddl-auto=validate`.

```text
V1   Initial schema
V2   Initial player placeholder
V3   Challenge catalogue (78 definitions)
V4   Henrik synchronization preparation
V5   Tracked players
V6   Optional season dates
V7   Extended team identifier
V8   Removal of the obsolete deep-sync task
V9   Query-supporting indexes
V10  Read-query optimizations
V11  Re-categorization of newly recognized game modes
V12  Backfill of round averages for the re-categorized modes
V13  Reset of every table derived from Henrik data
V14  Removal of challenges filtered on a retired game mode (78 → 62)
V15  Per-player, per-season synchronization completion flag
V16  Stop reason on per-player synchronization results
```

Never edit an applied migration. Add a new migration for every schema or reference-data change. Column-level detail is
in [`../docs/data-model.md`](../docs/data-model.md).

To reset the local database:

```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT
ALL
ON SCHEMA public TO valorant;
GRANT ALL
ON SCHEMA public TO public;
```

## Main API routes

Parameters, response shapes and failure modes are documented in [`docs/api.md`](docs/api.md).

### Public routes

```http
GET /api/challenges/current
GET /api/rankings/current
GET /api/rankings/history?page&size
GET /api/players
GET /api/players/{playerId}?seasonId&gameMode
GET /api/players/{playerId}/matches?page&size&seasonId&map&agent&result&gameMode
GET /api/seasons
```

### Administrative routes

All administrative routes require `X-Admin-Key`.

```http
POST /api/admin/synchronizations
POST /api/admin/players/{playerId}/synchronizations
POST /api/admin/challenges/progress/recalculation
POST /api/admin/rankings/recalculation
GET  /api/admin/synchronizations/latest
GET  /api/admin/synchronizations?page&size
GET  /api/admin/synchronizations/{synchronizationId}
```

## Configuration reference

| Variable                              | Default                                             | Purpose                               |
| ------------------------------------- | --------------------------------------------------- | ------------------------------------- |
| `DB_URL`                              | `jdbc:postgresql://localhost:5432/valorant_tracker` | PostgreSQL JDBC URL                   |
| `DB_USERNAME`                         | `valorant`                                          | Database user                         |
| `DB_PASSWORD`                         | `valorant`                                          | Database password                     |
| `ADMIN_API_KEY`                       | required, at least 32 characters                    | Protects `/api/admin/**`              |
| `FRONTEND_ORIGIN`                     | `http://localhost:4200`                             | Allowed Angular origin                |
| `HENRIK_API_BASE_URL`                 | `https://api.henrikdev.xyz`                         | Henrik API base URL                   |
| `HENRIK_API_KEY`                      | required                                            | Henrik API key                        |
| `HENRIK_API_REGION`                   | `eu`                                                | Valorant region                       |
| `HENRIK_API_PLATFORM`                 | `pc`                                                | Valorant platform                     |
| `HENRIK_API_MAX_ATTEMPTS`             | `2`                                                 | Total request attempts                |
| `HENRIK_API_RETRY_DELAY`              | `PT60S`                                             | Minimum retry delay                   |
| `HENRIK_API_REQUESTS_PER_MINUTE`      | `28`                                                | Shared request limit                  |
| `HENRIK_API_CONNECT_TIMEOUT`          | `PT5S`                                              | HTTP connect timeout                  |
| `HENRIK_API_READ_TIMEOUT`             | `PT20S`                                             | HTTP read timeout                     |
| `HENRIK_API_RATE_LIMIT_SAFETY_MARGIN` | `PT0.1S`                                            | Rate-limit safety margin              |
| `STANDARD_SYNC_ENABLED`               | `true`                                              | Enables the standard sync scheduler   |
| `STANDARD_SYNC_CRON`                  | `0 0 6,12,18 * * *`                                 | Standard sync schedule                |
| `SCHEDULING_ZONE`                     | `Europe/Paris`                                      | Time zone for scheduled jobs          |
| `WEEK_ROLLOVER_ENABLED`               | `true`                                              | Enables the weekly rollover scheduler |
| `WEEK_ROLLOVER_CRON`                  | `0 5 0 * * MON`                                     | Weekly rollover schedule              |
| `WEEK_ROLLOVER_ZONE`                  | `UTC`                                               | Time zone for all weekly calculations |

## Release-candidate validation

Before publishing a release, validate these flows against a dedicated PostgreSQL database and a valid Henrik API key:

1. synchronize one player, then all active players;
2. verify that one player failure does not interrupt the remaining synchronizations;
3. run the same synchronization twice and confirm that no match is duplicated;
4. interrupt a synchronization mid-walk and confirm the next run re-walks the incomplete season in full;
5. recalculate challenge progress and confirm that ranking recalculation follows;
6. exercise public pagination, filters, empty responses and unknown-resource errors;
7. execute a week rollover with a fixed clock and verify that finalized history remains unchanged;
8. parse and execute every enabled production challenge definition;
9. verify Swagger authorization and every protected administrative route.

External Henrik calls are intentionally excluded from automated tests because they depend on credentials, rate limits
and live upstream data.

## Synchronization benchmark

A real end-to-end benchmark of the six-player standard synchronization is available once the application and PostgreSQL
are running. Run it from the repository root:

```bash
ADMIN_KEY="$ADMIN_API_KEY" RUNS=3 ./scripts/benchmark-full-synchronization.sh
```

Results are written to `target/full-synchronization-benchmark.csv`. Any non-200 response stops the benchmark and prints
the upstream error body.

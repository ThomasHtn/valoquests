# Backend

Java 25 / Spring Boot API for Valorant Tracker. See the [root README](../README.md) for the project overview and the
[frontend README](../frontend/README.md) for the Angular application.

## Architecture

The backend follows a feature-oriented architecture. Each domain owns its controllers, services, repositories, entities,
DTOs and business models.

```text
io.github.thomashtn.valorant.tracker
├── challenge        Weekly selection and progress calculation
├── henrik           External API clients, mapping, retry and rate limiting
├── match            Seasons, matches, statistics and idempotent imports
├── player           Tracked accounts, profiles and Riot account resolution
├── ranking          Weekly scores, positions and ranking history
├── synchronization  Standard and deep synchronization orchestration
└── shared           Security, errors, auditing and common web components
```

The project favors thin controllers, explicit transactions, constructor injection, immutable API DTOs and database
constraints for critical invariants.

## Synchronization flow

### Standard synchronization

1. Load an active tracked player.
2. Resolve the Riot PUUID when necessary.
3. Update the current competitive rank.
4. Retrieve recent match-history pages.
5. Import completed and previously unknown matches.
6. Recalculate weekly challenge progress.
7. Recalculate the ranking.
8. Store the successful synchronization timestamp.

### Deep synchronization

Deep synchronization explores older pages for initial imports or data recovery.

Its range is controlled by `DEEP_SYNC_SCOPE`:

- `CURRENT_SEASON` imports the current competitive season;
- `ALL_HISTORY` explores every available page.

A page limit and request delay prevent runaway processing and reduce pressure on the Henrik API.

## Challenge engine

Challenge definitions are stored as versioned JSON rules in PostgreSQL.

The engine supports sums, occurrence counts, distinct values, grouped maximums, composite objectives, ratios and
consecutive streaks.

A compatibility test parses every production challenge and verifies that its rule can be executed by the calculator
registry. This prevents unsupported catalogue changes from reaching production.

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

This command runs Checkstyle, unit and integration tests, JaCoCo report generation and SpotBugs. Docker must be
available because the integration suite starts an isolated PostgreSQL 17 container.

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
V3   Challenge catalogue
V4   Henrik synchronization preparation
V5   Tracked players
V6   Optional season dates
V7   Extended team identifier
V8   Removal of the obsolete deep-sync task
V9   Query-supporting indexes
V10  Read-query optimizations
```

Never edit an applied migration. Add a new migration for every schema or reference-data change.

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

### Public routes

```http
GET /api/challenges/current
GET /api/rankings/current
GET /api/rankings/history
GET /api/players
GET /api/players/{playerId}
GET /api/players/{playerId}/matches
```

### Administrative routes

All administrative routes require `X-Admin-Key`.

```http
POST /api/admin/synchronizations
POST /api/admin/players/{playerId}/synchronizations
POST /api/admin/synchronizations/deep
POST /api/admin/players/{playerId}/synchronizations/deep
POST /api/admin/challenges/progress/recalculation
POST /api/admin/rankings/recalculation
GET  /api/admin/synchronizations/latest
GET  /api/admin/synchronizations
GET  /api/admin/synchronizations/{synchronizationId}
```

## Configuration reference

| Variable                         | Default                                             | Purpose                  |
|----------------------------------|-----------------------------------------------------|--------------------------|
| `DB_URL`                         | `jdbc:postgresql://localhost:5432/valorant_tracker` | PostgreSQL JDBC URL      |
| `DB_USERNAME`                    | `valorant`                                          | Database user            |
| `DB_PASSWORD`                    | `valorant`                                          | Database password        |
| `ADMIN_API_KEY`                  | required, at least 32 characters                    | Protects `/api/admin/**` |
| `FRONTEND_ORIGIN`                | `http://localhost:4200`                             | Allowed Angular origin   |
| `HENRIK_API_BASE_URL`            | `https://api.henrikdev.xyz`                         | Henrik API base URL      |
| `HENRIK_API_KEY`                 | required                                            | Henrik API key           |
| `HENRIK_API_REGION`              | `eu`                                                | Valorant region          |
| `HENRIK_API_PLATFORM`            | `pc`                                                | Valorant platform        |
| `HENRIK_API_MAX_ATTEMPTS`        | `2`                                                 | Total request attempts   |
| `HENRIK_API_RETRY_DELAY`         | `PT60S`                                             | Minimum retry delay      |
| `HENRIK_API_REQUESTS_PER_MINUTE` | `28`                                                | Shared request limit     |
| `DEEP_SYNC_SCOPE`                | `CURRENT_SEASON`                                    | Historical import range  |

## Release-candidate validation

Before publishing a release, validate these flows against a dedicated PostgreSQL database and a valid Henrik API key:

1. synchronize one player, then all active players;
2. verify that one player failure does not interrupt the remaining synchronizations;
3. run deep synchronization twice and confirm that no match is duplicated;
4. recalculate challenge progress and confirm that ranking recalculation follows;
5. exercise public pagination, filters, empty responses and unknown-resource errors;
6. execute a week rollover with a fixed clock and verify that finalized history remains unchanged;
7. parse and execute all 78 production challenge definitions;
8. verify Swagger authorization and every protected administrative route.

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

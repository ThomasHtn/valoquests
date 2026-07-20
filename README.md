# Valorant Tracker

Valorant Tracker is a personal full-stack portfolio project used to collect, store, analyse and compare Valorant
statistics for a fixed group of players.

The backend retrieves account, rank and match-history data from the Henrik API, stores normalized data in PostgreSQL and
exposes REST endpoints for the Angular application. Weekly challenge progression and ranking calculation are the next
functional milestone; the synchronization and persistence foundation is implemented first so those calculations can rely
on stable, reproducible data.

## Technology stack

- Java 25
- Spring Boot 4.0.6
- Spring MVC and WebClient
- Spring Data JPA and Hibernate
- PostgreSQL 17
- Flyway
- Spring Security
- Spring Boot Actuator
- Springdoc OpenAPI
- MapStruct
- Lombok
- Maven Wrapper
- JUnit 5, Mockito, AssertJ and MockWebServer

## Architecture

The backend uses feature-oriented packages. A feature owns its controllers, services, repositories, entities, DTOs and
domain models.

```text
io.github.thomashtn.valorant.tracker
├── challenge
├── henrik
├── match
├── player
├── ranking
├── synchronization
└── shared
```

Main responsibilities:

- `henrik`: HTTP clients, retry and rate-limit policies, external DTOs and mappings;
- `player`: tracked accounts, current rank and Riot account resolution;
- `match`: seasons, matches, player statistics per match and idempotent imports;
- `synchronization`: standard/deep orchestration, execution history and admin routes;
- `challenge`: challenge catalogue and future weekly progression logic;
- `ranking`: future weekly score and ranking logic;
- `shared`: cross-cutting configuration, security, errors, pagination and auditing.

## Synchronization behaviour

### Standard synchronization

A standard synchronization is optimized for frequent execution:

1. load the tracked player;
2. resolve the Riot PUUID when necessary;
3. retrieve and map the current competitive rank;
4. retrieve match-history pages in batches of 10;
5. import completed matches idempotently;
6. stop when Henrik returns an empty page, an incomplete page, or a full page containing no new player-match
   association;
7. update the player's last successful synchronization timestamp.

This behaviour imports more than the first page on an empty database while avoiding a complete history scan on every
scheduled execution.

### Deep synchronization

A deep synchronization browses older Henrik match-history pages. Its scope is configured with `DEEP_SYNC_SCOPE`:

- `CURRENT_SEASON`: stop at the first older-season boundary;
- `ALL_HISTORY`: continue until Henrik returns no more complete pages.

A safety page limit prevents infinite loops caused by an unexpected external API response.

### Import idempotence

The database and services prevent duplicates through the following constraints:

- `valorant_match.external_match_id` is unique;
- `(player_id, match_id)` is unique in `player_match`;
- existing player-match associations are skipped;
- invalid, incomplete and unrelated Henrik match payloads are ignored and logged.

## Logs

Synchronization logs provide enough context to diagnose missing pages without enabling verbose HTTP-body logging. Each
imported page records:

- player identifier;
- page index and Henrik `start` offset;
- requested and received match counts;
- matches eligible for import;
- matches actually imported;
- cumulative imported count;
- explicit pagination stop reason.

Default application logs use `INFO`. Detailed import decisions are available at `DEBUG` for the match import service.

Example temporary configuration:

```properties
logging.level.io.github.thomashtn.valorant.tracker.match.service.MatchImportService=DEBUG
```

Never enable full Henrik response-body logging in production because payloads are large and may expose account data.

## Prerequisites

- JDK 25
- Docker and Docker Compose, or PostgreSQL 17
- a Henrik API key

The Maven Wrapper downloads Maven 3.8.7 on first use. An internet connection is therefore required the first time
`./mvnw` is executed.

## Local configuration

Copy the environment template:

```bash
cp .env.example .env
```

Required values:

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/valorant_tracker
DB_USERNAME=valorant
DB_PASSWORD=change-me
ADMIN_API_KEY=replace-with-a-long-random-secret
HENRIK_API_KEY=your-henrik-api-key
```

The `.env` file is ignored by Git. IntelliJ IDEA can load it with an environment-file plugin, or the variables can be
exported in the shell.

## Start PostgreSQL

```bash
docker compose up -d
```

Check the container:

```bash
docker compose ps
```

## Run the backend

```bash
./mvnw spring-boot:run
```

The backend starts on `http://localhost:8080`.

Useful URLs:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`
- Health endpoint: `http://localhost:8080/actuator/health`

Use Swagger's **Authorize** action to provide the configured `X-Admin-Key` value for administrative routes.

## Build and tests

Run the complete verification pipeline:

```bash
./mvnw clean verify
```

Run unit tests only:

```bash
./mvnw test
```

Run one test class:

```bash
./mvnw -Dtest=PlayerSynchronizationServiceTest test
```

## Database migrations

Flyway is the only schema-management mechanism. Hibernate uses `ddl-auto=validate` and must never create or update
production tables.

Current migrations:

```text
V1__create_schema.sql
V2__insert_players.sql
V3__insert_challenges.sql
V4__prepare_henrik_synchronization.sql
V5__insert_players.sql
V6__allow_unresolved_season_dates.sql
V7__increase_player_match_team_id_length.sql
V8__remove_unused_deep_synchronization_task.sql
```

Do not edit an applied migration. Add a new versioned migration for every schema or reference-data change.

To reset the local database completely:

```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT
ALL
ON SCHEMA public TO valorant;
GRANT ALL
ON SCHEMA public TO public;
```

Restart the application afterward so Flyway can replay every migration.

## Administrative synchronization endpoints

All routes below require `X-Admin-Key`.

```http
POST /api/admin/synchronizations
POST /api/admin/players/{playerId}/synchronizations
POST /api/admin/synchronizations/deep
POST /api/admin/players/{playerId}/synchronizations/deep
GET  /api/admin/synchronizations/latest
GET  /api/admin/synchronizations?page=0&size=20
GET  /api/admin/synchronizations/{synchronizationId}
```

Example:

```bash
curl -X POST \
  -H "X-Admin-Key: ${ADMIN_API_KEY}" \
  http://localhost:8080/api/admin/players/1/synchronizations
```

## Configuration reference

| Variable                              | Default                                             | Purpose                          |
|---------------------------------------|-----------------------------------------------------|----------------------------------|
| `DB_URL`                              | `jdbc:postgresql://localhost:5432/valorant_tracker` | PostgreSQL JDBC URL              |
| `DB_USERNAME`                         | `valorant`                                          | Database user                    |
| `DB_PASSWORD`                         | `valorant`                                          | Database password                |
| `ADMIN_API_KEY`                       | local development value                             | Protects `/api/admin/**`         |
| `FRONTEND_ORIGIN`                     | `http://localhost:4200`                             | Allowed Angular origin           |
| `HENRIK_API_BASE_URL`                 | `https://api.henrikdev.xyz`                         | Henrik API base URL              |
| `HENRIK_API_KEY`                      | empty                                               | Henrik API key                   |
| `HENRIK_API_REGION`                   | `eu`                                                | Shared Valorant region           |
| `HENRIK_API_PLATFORM`                 | `pc`                                                | Shared platform                  |
| `HENRIK_API_MAX_ATTEMPTS`             | `2`                                                 | Initial request plus retry count |
| `HENRIK_API_RETRY_DELAY`              | `PT60S`                                             | Minimum retry delay              |
| `HENRIK_API_REQUESTS_PER_MINUTE`      | `28`                                                | Shared request limiter           |
| `HENRIK_API_RATE_LIMIT_SAFETY_MARGIN` | `PT0.1S`                                            | Additional request spacing       |
| `DEEP_SYNC_SCOPE`                     | `CURRENT_SEASON`                                    | Deep import range                |

## Development rules

- keep code, comments, Javadoc, logs and documentation in English;
- use feature packages and preserve domain boundaries;
- prefer constructor injection;
- keep controllers thin and business rules in services;
- use records for immutable API DTOs when appropriate;
- never expose JPA entities directly from controllers;
- make external imports idempotent;
- add tests for every bug fix and business rule;
- avoid broad `catch (Exception)` blocks and silent failures;
- log identifiers and counts, not secrets or complete external payloads;
- use a new Flyway migration instead of changing an applied migration.

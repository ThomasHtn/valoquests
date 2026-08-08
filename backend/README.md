# Backend

The API server for Valorant Tracker. It stores data in a PostgreSQL database, fetches match data from the Henrik
API, and exposes everything to the [frontend](../frontend/README.md) over HTTP.

See the [root README](../README.md) for what the whole project does.

## 1. What the backend does

- talks to **Henrik** (an external service that provides Valorant match data) to import matches several times a day;
- stores players, matches, weekly challenges and rankings in a **PostgreSQL** database;
- calculates challenge progress and the weekly ranking;
- exposes this data through a **REST API** consumed by the frontend.

## 2. Requirements

Before starting, make sure you have:

- **JDK 25** installed;
- **Docker and Docker Compose** (recommended, to run PostgreSQL easily), or a PostgreSQL 17 server already running;
- a **Henrik API key** (a free key from the Henrik API service, used to fetch Valorant data).

All commands below assume you are in the `backend/` folder.

## 3. Installation and configuration

### 3.1 Create your configuration file

Copy the example configuration file:

```bash
cp .env.example .env
```

Then open `.env` and fill in at least these values:

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/valorant_tracker
DB_USERNAME=valorant
DB_PASSWORD=change-me
ADMIN_API_KEY=replace-with-a-long-random-secret
HENRIK_API_KEY=your-henrik-api-key
```

- `ADMIN_API_KEY` protects administrative actions (like triggering a manual synchronization). Choose a long random
  value; it is not provided by Henrik, you invent it yourself.
- `HENRIK_API_KEY` is required for the application to fetch any Valorant data. Without it, synchronization fails.

### 3.2 Start the database

If you use Docker:

```bash
docker compose up -d
docker compose ps   # check that the "postgres" service is healthy
```

If you already run your own PostgreSQL 17 instance, make sure `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` in `.env`
match it instead.

## 4. Running the backend

The first run downloads Maven automatically, so an internet connection is required at least once.

```bash
./mvnw spring-boot:run
```

Once it is running:

| Purpose            | URL                                          |
| ------------------- | --------------------------------------------- |
| API                 | `http://localhost:8080/api/...`               |
| Interactive API docs (Swagger) | `http://localhost:8080/swagger-ui.html`  |
| Raw API description | `http://localhost:8080/api-docs`             |
| Health check        | `http://localhost:8080/actuator/health`       |

In Swagger UI, use the **Authorize** button to enter your `ADMIN_API_KEY` and test administrative routes.

## 5. Running tests

```bash
./mvnw test              # fast tests only
./mvnw verify             # full quality gate: tests, coverage check, code style, static analysis
```

`./mvnw verify` needs Docker to be available, because part of the test suite starts a temporary PostgreSQL
container. Without Docker, those tests are skipped rather than failed.

To run a single test class:

```bash
./mvnw -Dtest=PlayerSynchronizationServiceTest test
```

## 6. Database changes

Every change to the database structure is done through a **Flyway migration** (a numbered SQL file). Migrations that
have already run must never be edited — a new migration is added instead. This keeps every environment's database in
sync and history accurate.

To fully reset your local database (destroys all local data):

```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO valorant;
GRANT ALL ON SCHEMA public TO public;
```

## 7. Main API routes

Full details (parameters, response shape, error cases) are available in the interactive Swagger UI
(`http://localhost:8080/swagger-ui.html`) once the backend is running.

### Public routes (no key required)

```http
GET /api/challenges/current
GET /api/rankings/current
GET /api/rankings/history?page&size
GET /api/players
GET /api/players/{playerId}?seasonId&gameMode
GET /api/players/{playerId}/matches?page&size&seasonId&map&agent&result&gameMode
GET /api/seasons
```

### Administrative routes (require the `X-Admin-Key` header)

```http
POST /api/admin/synchronizations
POST /api/admin/players/{playerId}/synchronizations
POST /api/admin/challenges/progress/recalculation
POST /api/admin/rankings/recalculation
GET  /api/admin/synchronizations/latest
GET  /api/admin/synchronizations?page&size
GET  /api/admin/synchronizations/{synchronizationId}
```

## 8. Configuration reference

| Variable                              | Default                                             | What it controls                        |
| -------------------------------------- | ---------------------------------------------------- | ----------------------------------------- |
| `DB_URL`                               | `jdbc:postgresql://localhost:5432/valorant_tracker` | Database connection address              |
| `DB_USERNAME`                          | `valorant`                                          | Database user                            |
| `DB_PASSWORD`                          | `valorant`                                          | Database password                        |
| `ADMIN_API_KEY`                        | required, at least 32 characters                    | Protects `/api/admin/**` routes          |
| `FRONTEND_ORIGIN`                      | `http://localhost:4200`                             | The only website address allowed to call this API |
| `HENRIK_API_BASE_URL`                  | `https://api.henrikdev.xyz`                         | Address of the Henrik API                |
| `HENRIK_API_KEY`                       | required                                            | Your Henrik API key                      |
| `HENRIK_API_REGION`                    | `eu`                                                | Valorant region used for all players     |
| `HENRIK_API_PLATFORM`                  | `pc`                                                | Valorant platform used for all players   |
| `HENRIK_API_MAX_ATTEMPTS`              | `4`                                                  | Retries on a temporary Henrik failure    |
| `HENRIK_API_RETRY_DELAY`               | `PT60S`                                             | Minimum wait before retrying             |
| `HENRIK_API_RATE_LIMIT_MAX_ATTEMPTS`   | `25`                                                 | Retries when Henrik enforces rate limiting |
| `HENRIK_API_REQUESTS_PER_MINUTE`       | `28`                                                 | Maximum requests sent to Henrik per minute |
| `HENRIK_API_CONNECT_TIMEOUT`           | `PT5S`                                              | Connection timeout                       |
| `HENRIK_API_READ_TIMEOUT`              | `PT20S`                                             | Response timeout                         |
| `HENRIK_API_RATE_LIMIT_SAFETY_MARGIN`  | `PT0.1S`                                            | Small extra delay between requests       |
| `STANDARD_SYNC_ENABLED`                | `true`                                              | Turns automatic synchronization on/off   |
| `STANDARD_SYNC_CRON`                   | `0 0 6,12,18 * * *`                                 | When automatic synchronization runs (06:00, 12:00, 18:00) |
| `SCHEDULING_ZONE`                      | `Europe/Paris`                                      | Time zone for the schedule above         |
| `WEEK_ROLLOVER_ENABLED`                | `true`                                              | Turns the automatic weekly reset on/off  |
| `WEEK_ROLLOVER_CRON`                   | `0 5 0 * * MON`                                     | When the week resets (Monday 00:05)      |
| `WEEK_ROLLOVER_ZONE`                   | `Europe/Paris`                                      | Time zone used for weekly calculations (week boundaries, boss selection) and the rollover cron |

`PT5S`, `PT60S`, etc. are ISO-8601 durations (`PT5S` = 5 seconds, `PT60S` = 60 seconds).

## 9. Common problems

| Problem                                                      | Likely cause / fix                                                                    |
| -------------------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| `./mvnw spring-boot:run` fails with a database connection error | PostgreSQL is not running or `.env` values don't match it. Run `docker compose ps`.       |
| Requests to `/api/admin/**` return an authorization error       | The `X-Admin-Key` header is missing, or does not match `ADMIN_API_KEY` in `.env`.        |
| No player data appears after starting                          | Nothing has been synchronized yet, or `HENRIK_API_KEY` is missing/invalid.               |
| `./mvnw verify` skips some tests                                | Docker is not available; part of the test suite needs it to start a test database.        |
| The frontend can't reach the API (CORS error)                  | `FRONTEND_ORIGIN` in `.env` doesn't match the address the frontend runs on.               |

## 10. Going further

This README covers setup and day-to-day commands. For the big picture (what the whole project does, how the
backend and frontend fit together), see the [root README](../README.md).

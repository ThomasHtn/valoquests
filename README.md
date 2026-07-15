# Valorant Tracker Backend

Backend foundation for the Valorant Tracker portfolio project. The project provides the technical structure, persistence model, API DTOs, repositories, database migrations, configuration, administrative API protection, and shared error handling. Business rules are intentionally left unimplemented.

## Technology stack

- Java 25
- Spring Boot 4.0.6
- PostgreSQL 17
- Spring Data JPA
- Spring Validation
- Spring Security
- Spring Boot Actuator
- Flyway
- MapStruct
- Lombok
- Maven

## Project scope

The backend is prepared to support:

- incremental and deep synchronization with the Henrik API;
- storage of players, matches, weekly challenges, progress snapshots, rankings, and synchronization history;
- weekly statistics and challenge progress without a dedicated `week` table;
- public endpoints consumed by the Angular frontend;
- protected administrative endpoints used by schedulers and manual operations.

No business service implementation or REST controller is included yet. Service interfaces and DTOs define the expected boundaries for future implementation.

## Package organization

The code is organized by feature:

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

Each feature owns its persistence entities, repositories, DTOs, models, and service contracts. Cross-cutting configuration, exceptions, shared DTOs, and common persistence behavior belong to `shared`.

## Local setup

### Prerequisites

- JDK 25
- Maven 3.9 or later
- Docker and Docker Compose, or a local PostgreSQL 17 instance

### Environment variables

Copy the example environment file and replace the development values when required:

```bash
cp .env.example .env
```

Main variables:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
ADMIN_API_KEY
HENRIK_API_BASE_URL
HENRIK_API_KEY
FRONTEND_ORIGIN
```

The administrative key must never be committed to the repository.

### Start PostgreSQL

```bash
docker compose up -d
```

### Run the application

```bash
mvn spring-boot:run
```

### Run verification

```bash
mvn clean verify
```

## Database management

Flyway is the only source of truth for the database schema. Hibernate uses `ddl-auto=validate` and therefore validates mappings without creating or modifying tables.

Available migrations:

1. `V1__create_schema.sql` creates the complete schema and idempotency constraints.
2. `V2__insert_players.sql` is the placeholder for the predefined tracked players.
3. `V3__insert_challenges.sql` inserts the challenge library supplied with the project.

Weekly records are identified by `week_start`. The application does not use a dedicated `week` entity or table.

## Administrative API protection

Every request under `/api/admin/**` must include the following header:

```http
X-Admin-Key: <ADMIN_API_KEY>
```

The key is loaded from an environment variable. Missing and invalid keys are rejected before the request reaches a controller.

## Code quality conventions

- All source code, comments, JavaDoc, configuration comments, and documentation are written in English.
- Every declared function must include JavaDoc explaining its responsibility.
- Formatting uses four spaces and avoids compressed declarations.
- Business rules belong in services, not controllers or repositories.
- Database changes require a new Flyway migration.
- Public API payloads use dedicated DTOs rather than persistence entities.
- Time-dependent code should use the configured `Clock` bean to remain testable.

## Suggested implementation order

1. Complete the six predefined players migration.
2. Add Henrik API response DTOs and client operations.
3. Implement incremental match synchronization.
4. Implement statistics and challenge calculators.
5. Implement ranking snapshots and weekly finalization.
6. Add REST controllers for the agreed API routes.
7. Add integration tests with Testcontainers.

## Current limitations

- Business services are contracts only.
- The Henrik client contains only the minimum placeholder models.
- REST controllers are not implemented.
- Player seed values must be completed.
- The project requires JDK 25 to build with the configured compiler release.

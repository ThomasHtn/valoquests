# Backend architecture

Internal structure of the Spring Boot module: how code is organized, where a decision may live, and which framework
behaviors are configured away from their defaults. System-level context is in
[`docs/architecture.md`](../../docs/architecture.md).

## Package layout

Top-level packages are business features, not technical layers. The rationale is in
[ADR 0002](../../docs/adr/0002-feature-oriented-packages.md).

```text
io.github.thomashtn.valorant.tracker
├── challenge        Weekly selection, rule parsing, progress calculation
│   ├── calculator   Seven progress calculators, the registry, the evaluation context
│   ├── controller   Public read route and administrative recalculation route
│   ├── entity       Challenge, WeeklyChallenge, PlayerChallengeProgress
│   ├── model        Typed rule vocabulary (metric, operator, difficulty, mode…)
│   ├── parser       JSON rule deserialization
│   ├── repository   Catalogue, weekly selections, progress
│   └── service      Selection, calculation, recalculation, persistence
├── henrik           External API boundary
│   ├── client       WebClient clients, request executor, rate limiter, retry, error mapping
│   ├── config       Typed properties and WebClient wiring
│   ├── dto          Transport objects mirroring Henrik payloads
│   ├── exception    One exception per upstream failure mode
│   └── mapper       Transport → entity translation
├── match            Seasons, matches, statistics and idempotent import
├── player           Tracked accounts, profiles, Riot account resolution
├── ranking          Weekly scores, positions, ranking history
├── synchronization  Import orchestration, scheduling and monitoring
├── week             Week calendar and weekly rollover
└── shared           Security, error handling, auditing, common web types
```

`shared` holds only what genuinely has no feature owner: the security filter chain, the global exception handler, the
paginated response envelope, the auditable entity base class and the framework configuration beans.

## Layering

```text
Controller  →  Service  →  Repository  →  PostgreSQL
    ↓             ↓
   DTO       business rule
```

| Layer      | May do                                                             | May not do                                        |
| ---------- | ------------------------------------------------------------------ | -------------------------------------------------- |
| Controller | Bind parameters, delegate, return a DTO, declare OpenAPI metadata   | Decide anything, touch a repository or an entity    |
| Service    | Own every business rule, validate, orchestrate, define transactions | Speak HTTP                                          |
| Repository | Query and persist                                                  | Decide                                              |
| Mapper     | Translate between shapes                                            | Apply a business rule                               |

Additional rules the code follows consistently:

- **Constructor injection only.** No field injection, no setter injection.
- **DTOs are immutable records** and are distinct from entities. Collections are defensively copied in compact
  constructors, so a response cannot be mutated after construction.
- **Interfaces exist where a seam is needed**, not by convention. A `Default*` class paired with an interface means a
  second implementation or a test double justified it; several services are plain concrete classes on purpose.
- **Validation lives in services**, so an invalid value produces a described 400 rather than a framework binding
  failure the API cannot explain. `MatchHistoryFilter` deliberately carries raw request strings for that reason.

## Transactions

The transaction boundary is a decision, and it differs by feature:

| Component                              | Boundary                    | Why                                                                     |
| -------------------------------------- | --------------------------- | ------------------------------------------------------------------------ |
| Query services                          | `@Transactional(readOnly)` | Consistent reads without a write lock                                    |
| `DefaultWeeklyRolloverService`          | `@Transactional`           | Finalizing the old week must not survive a failure to create the new pack |
| `DefaultChallengeRecalculationService`  | `@Transactional`           | A week's progress is rebuilt as a unit                                   |
| `DefaultRankingRecalculationService`    | `@Transactional`           | Positions are only coherent as a complete set                            |
| `DefaultWeeklyChallengeSelectionService`| `@Transactional`           | A pack is created as a unit                                              |
| Synchronization services                | **none**                   | See below                                                                |

`DefaultSynchronizationCommandService`, `PlayerSynchronizationService`, `SeasonMatchHistoryWalker` and
`SeasonSynchronizationStateService` are deliberately non-transactional. Wrapping them would keep a connection checked
out across every Henrik call and would break the per-season completion flag that guarantees an interrupted import
leaves no permanent hole. The constraint is stated in the Javadoc of all four classes because it is invisible at the
call site; the reasoning is in [ADR 0005](../../docs/adr/0005-non-transactional-synchronization.md).

## Persistence

- Flyway owns the schema; Hibernate runs `ddl-auto=validate`. See
  [ADR 0003](../../docs/adr/0003-flyway-owns-the-schema.md).
- `spring.jpa.open-in-view=false`. Lazy associations must be loaded inside the service, not during response
  serialization.
- `hibernate.jdbc.time_zone=UTC`. Instants are stored and read in UTC.
- `hibernate.default_batch_fetch_size=50` bounds the N+1 cost of loading lazy associations for a group of rows.
- `AuditableEntity` supplies `created_at` / `updated_at` to every entity.
- Monetary-like values - challenge progress, targets, statistics - use `BigDecimal` and `NUMERIC`, never floating
  point, because they decide points.

## Configuration

Configuration is bound to typed records rather than read through `@Value`, so an invalid value fails at startup.

| Prefix                | Type                    | Holds                                                        |
| --------------------- | ----------------------- | ------------------------------------------------------------ |
| `app`                 | `ApplicationProperties` | Frontend origin, admin API key                                |
| `app.scheduling`      | `ApplicationProperties` | Job enablement, cron expressions, zones                       |
| `henrik.api`          | `HenrikApiProperties`   | Base URL, key, region, platform, timeouts, retry, rate limit  |

Every value is overridable by environment variable, and `spring-dotenv` loads a local `.env` during development. The
full reference table is in the [backend README](../README.md#configuration-reference).

`TimeConfig` exposes the application `Clock` as a bean so weekly behavior can be tested with a fixed clock rather than
by freezing system time.

## Error handling

`GlobalExceptionHandler` maps every escaping exception to an `ApiErrorResponse`:

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "code": "RESOURCE_NOT_FOUND",
  "detail": "Player 42 does not exist.",
  "instance": "/api/players/42",
  "timestamp": "2026-08-01T09:00:00Z",
  "errors": { "size": "size must be between 1 and 100" }
}
```

`errors` is present only for field-level validation failures; null-valued properties are omitted globally through
`spring.jackson.default-property-inclusion=non_null`.

| Exception                              | Status | `code`                       |
| -------------------------------------- | ------ | ---------------------------- |
| `ResourceNotFoundException`            | 404    | `RESOURCE_NOT_FOUND`         |
| `NoResourceFoundException`             | 404    | `RESOURCE_NOT_FOUND`         |
| `MethodArgumentNotValidException`      | 400    | `VALIDATION_FAILED`          |
| `InvalidRequestException`              | 400    | `INVALID_ARGUMENT`           |
| `MethodArgumentTypeMismatchException`  | 400    | `INVALID_ARGUMENT`           |
| `HttpRequestMethodNotSupportedException` | 405  | `METHOD_NOT_ALLOWED`         |
| `HenrikRateLimitException`             | 429    | `HENRIK_RATE_LIMIT_EXCEEDED` |
| `HenrikApiException`                   | 502    | `HENRIK_API_ERROR`           |
| Anything else                          | 500    | `INTERNAL_ERROR`             |

An upstream failure surfaces as 502 rather than 500 on purpose: the caller learns the dependency failed, not that the
application is broken.

The admin key filter answers outside this handler because it runs before the dispatcher, and emits the same envelope
with `ADMIN_KEY_MISSING` or `ADMIN_KEY_INVALID`.

## Security

```java
.csrf(disable)                                   // no cookie carries authority
.cors(single configured origin)
.sessionManagement(STATELESS)
.requestMatchers(actuator health/info, swagger, api-docs).permitAll()
.requestMatchers(GET, "/api/**").permitAll()
.requestMatchers("/api/admin/**").permitAll()    // delegated to AdminApiKeyFilter
.anyRequest().denyAll()
```

`AdminApiKeyFilter` runs before `UsernamePasswordAuthenticationFilter` and short-circuits any `/api/admin` request
without a valid `X-Admin-Key`. Everything unmatched is denied, so a new endpoint is closed until a rule opens it. See
[ADR 0007](../../docs/adr/0007-admin-api-key.md).

## Observability

- Actuator exposes `health`, `info` and `metrics` only.
- Logs carry identifiers and counts, never secrets or complete upstream payloads.
- Synchronization writes its own audit trail to the database: one execution row and one result row per player,
  including the reason each player's match-history walk stopped.

## Quality gate

`./mvnw clean verify` runs, in order:

1. **Checkstyle** (`validate` phase, fails the build on violation, test sources included);
2. **unit tests** on H2 in PostgreSQL compatibility mode;
3. **integration tests** tagged `integration`, on a `postgres:17-alpine` Testcontainer with Flyway enabled;
4. **JaCoCo** report and coverage check - 90 % line, 70 % branch;
5. **SpotBugs**.

The same command runs in CI on every push to `main` or `develop` and on every pull request to `main`.

# 0003. Flyway is the only schema authority

## Status

Accepted

## Context

Hibernate can generate and update a schema from mapped entities. That is convenient in development and unusable in
production: `ddl-auto=update` never drops anything, never renames anything, and silently diverges between environments
until a deployment fails on a constraint nobody wrote.

The application also needs data migrations, not only schema ones - reclassifying matches whose queue was previously
unrecognized, deleting challenges filtered on a retired game mode, resetting derived data after a rule change. None of
that is expressible as an entity mapping.

## Decision

Flyway owns the schema. Hibernate runs with `ddl-auto=validate` and only checks that the mapped entities agree with the
migrated schema at startup.

- Every schema change and every reference-data change is a new versioned file in
  `backend/src/main/resources/db/migration`.
- An applied migration is never edited. `spring.flyway.validate-on-migrate=true` enforces this by refusing to start
  when a checksum changed.
- Constraints are preferred over application-only validation: uniqueness, foreign keys and defaults live in the schema
  where they cannot be bypassed.

Integration tests run the full migration chain against a `postgres:17-alpine` Testcontainer with `ddl-auto=validate`,
so a mapping that drifts from the schema fails the build rather than production startup.

## Consequences

- The schema has a readable history. `V13` explains, in the migration itself, why every derived table was truncated;
  `V11` explains why one specific match was corrected by identifier rather than by rule.
- Migrations are the right place for reasoning, and they are used that way. Several carry more comment than SQL,
  because the SQL is trivial and the decision is not.
- Fixing a mistake means writing another migration. There is no shortcut, and that is the point.
- Unit tests pay a small inconsistency for speed: they run on H2 with `create-drop` and Flyway disabled, so the schema
  they see is Hibernate's rather than the migrated one. Only integration tests exercise the real schema, which is why
  anything constraint-dependent must be covered there.
- Reference data seeded by migration cannot be edited through the API. The challenge catalogue is deliberately not
  administrable at runtime - see [0004](0004-challenge-rules-as-json.md).

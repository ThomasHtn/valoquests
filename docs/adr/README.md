# Architecture decision records

Decisions that shaped Valorant Tracker, with the reasoning behind them and what they cost. A record exists here when
the decision is not obvious from reading the code, when a reasonable reviewer would ask "why not the other way", or
when reversing it would be expensive.

These records were written retrospectively from the implemented codebase, so they carry no decision date. Each one
describes the state the code is actually in; when a decision is revisited, the record is superseded rather than edited.

| #                                              | Decision                                                     | Status   |
| ---------------------------------------------- | ------------------------------------------------------------ | -------- |
| [0001](0001-monorepo.md)                       | Keep backend and frontend in one repository                   | Accepted |
| [0002](0002-feature-oriented-packages.md)      | Organize backend code by business feature                     | Accepted |
| [0003](0003-flyway-owns-the-schema.md)         | Flyway is the only schema authority                           | Accepted |
| [0004](0004-challenge-rules-as-json.md)        | Store challenge rules as versioned JSON in PostgreSQL         | Accepted |
| [0005](0005-non-transactional-synchronization.md) | Run synchronization outside any database transaction       | Accepted |
| [0006](0006-single-week-calendar.md)           | Anchor every weekly calculation on one shared calendar        | Accepted |
| [0007](0007-admin-api-key.md)                  | Protect write operations with a shared admin key              | Accepted |
| [0008](0008-season-scoped-history.md)          | Bound match-history import to the current season              | Accepted |
| [0009](0009-recompute-instead-of-accumulate.md) | Recompute derived data instead of accumulating it            | Accepted |
| [0010](0010-signal-based-zoneless-frontend.md) | Build the frontend zoneless on signals and `httpResource`      | Accepted |
| [0011](0011-domain-oriented-frontend.md)       | Organize frontend code by domain, not by technical type       | Accepted |

## Template

```markdown
# NNNN. Title

## Status

Accepted | Superseded by [NNNN](...)

## Context

The forces at play. What made a decision necessary.

## Decision

What was decided, stated in the present tense.

## Consequences

What this buys, what it costs, and what it forbids.
```

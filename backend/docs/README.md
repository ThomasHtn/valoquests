# Backend documentation

Detailed documentation for the Spring Boot module. Start with the [backend README](../README.md) for setup, build and
test commands.

| Document                                    | Covers                                                                     |
| ------------------------------------------- | -------------------------------------------------------------------------- |
| [Architecture](architecture.md)             | Package layout, layering rules, transactions, configuration, error handling |
| [Synchronization](synchronization.md)       | The Henrik import pipeline, season scope, idempotency and failure isolation |
| [Challenge engine](challenge-engine.md)     | Rule format, the seven calculators, weekly selection and progress rules     |
| [API](api.md)                               | Every route, its parameters, its response shape and its failure modes       |

Project-wide context lives in [`docs/`](../../docs): [architecture](../../docs/architecture.md),
[domain model](../../docs/domain-model.md), [data model](../../docs/data-model.md) and the
[decision records](../../docs/adr/README.md).

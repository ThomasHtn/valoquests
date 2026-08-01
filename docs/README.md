# Documentation

Project-wide documentation for Valorant Tracker. Module-specific documentation lives next to the module it describes.

## Global

| Document                            | Covers                                                                     |
| ----------------------------------- | -------------------------------------------------------------------------- |
| [Architecture](architecture.md)     | System components, runtime topology, request and job flows, boundaries      |
| [Domain model](domain-model.md)     | Business concepts, weekly lifecycle and the invariants the code protects    |
| [Data model](data-model.md)         | PostgreSQL tables, constraints, indexes and migration history               |
| [Decisions](adr/README.md)          | Architecture decision records: what was chosen, why, and what it costs      |

## Modules

| Module                                    | Documentation                        |
| ----------------------------------------- | ------------------------------------ |
| [Backend](../backend/README.md)           | [`backend/docs`](../backend/docs)    |
| [Frontend](../frontend/README.md)         | [`frontend/docs`](../frontend/docs)  |

## Design material

- [`frontend/docs/images`](../frontend/docs/images) holds the UI mockups used as the source of truth for each screen.
- [`frontend/docs/preview`](../frontend/docs/preview) holds screenshots captured from the running application.
- [`frontend/docs/boss.md`](../frontend/docs/boss.md) specifies the weekly-boss scoring model. It is a design document
  for a future iteration and describes no implemented behavior.

## Writing rules

Documentation is written in English and documents intent, invariants, decisions and API behavior. It does not restate
what the code already says: the codebase carries Javadoc and TSDoc on every public type, and these documents link the
pieces together rather than duplicating them.

When behavior changes, the document describing that behavior changes in the same commit. A document that contradicts
the code is worse than no document, so a fact stated here must be verifiable by reading the code it describes.

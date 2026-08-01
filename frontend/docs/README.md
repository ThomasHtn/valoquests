# Frontend documentation

Detailed documentation for the Angular module. Start with the [frontend README](../README.md) for setup and commands.

| Document                        | Covers                                                                        |
| ------------------------------- | ------------------------------------------------------------------------------ |
| [Architecture](architecture.md) | Directory layout, layering rules, routing, i18n, styling                        |
| [Conventions](conventions.md)   | Naming, signals, templates, accessibility and the rules a reviewer will apply    |
| [Data access](data-access.md)   | `httpResource`, shared vs. parameterized resources, and the state-handling guards |
| [Pages](pages.md)               | What each of the four screens shows, what it fetches and how it behaves          |

## Design material

- [`images/`](images) - UI mockups, the source of truth for each screen. Inspect the relevant mockup before
  implementing or changing a screen.
- [`preview/`](preview) - screenshots captured from the running application. Update the relevant screenshot whenever a
  previewed screen changes visibly.
- [`boss.md`](boss.md) - specification of a weekly-boss scoring model. **A design document for a future iteration; no
  part of it is implemented.**

Project-wide context lives in [`docs/`](../../docs), and the API this module consumes is documented in
[`backend/docs/api.md`](../../backend/docs/api.md).

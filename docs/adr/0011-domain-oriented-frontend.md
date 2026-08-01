# 0011. Organize frontend code by domain, not by technical type

## Status

Accepted

## Context

The Angular default is to group by technical type: `models/`, `services/`, `components/`, `pipes/`. It is easy to
explain and it scatters every feature across four directories. Adding a season filter to match history means editing
`models/season.ts`, `services/seasons.service.ts` and two components in unrelated folders, with nothing in the tree
indicating they belong together.

The same reasoning that shaped the backend packages ([0002](0002-feature-oriented-packages.md)) applies here, and
applying it consistently on both sides also makes the two easier to read side by side.

## Decision

Code is organized by domain, and four directories carry four distinct meanings.

```text
src/app/
├── core/     Domain logic and data access. No components, ever.
├── shared/   Presentational components reused by more than one page.
├── layout/   The application shell.
└── pages/    Routed screens, each lazy-loaded.
```

Layering rules, enforced by review rather than by tooling:

- `core/` holds models, data-access services and pure functions. It contains no component.
- `shared/` may import **types** from `core/`, never services. A shared component receives data through `input()`; it
  does not fetch it.
- `pages/` compose `core/` and `shared/`. **Pages never import from one another.**
- Anything used by exactly one page stays inside that page's folder - `overview/podium/`, `player-profile/
  match-day.utils.ts`.

A file's suffix states what it exports, so contents are predictable from the tree alone:

| Suffix           | Exports                                                       |
| ---------------- | ------------------------------------------------------------- |
| `*.model.ts`     | Types only, usually mirroring a backend DTO                    |
| `*.utils.ts`     | Pure functions: formatters, class resolvers, math              |
| `*.constants.ts` | Constant values only                                           |
| `*-api.ts`       | A `@Service` exposing `httpResource`-backed data access         |

Cross-folder imports use the path aliases `@core/*`, `@shared/*`, `@layout/*`, `@pages/*` and `@env/*` rather than
relative chains. Same-folder imports stay relative.

## Consequences

- A domain is one folder. Its model, its data access and its formatting helpers sit together.
- The "no page imports another page" rule keeps lazy-loaded route chunks genuinely independent: importing one page from
  another would silently pull both into the same bundle.
- A component that outgrows one page moves to `shared/` in a single step, because "used by exactly one page" is a
  mechanical test rather than a judgement call.
- `shared/` risks becoming a dumping ground, the same way `shared` does on the backend. The rule "used by more than one
  page" is the entry condition, and a component used once belongs to its page.
- The rules are conventions, not compile-time constraints. Nothing prevents a page from importing another page or a
  shared component from injecting a service; only review does.
- Path aliases must be kept in sync between `tsconfig.json` and any tool resolving modules independently.

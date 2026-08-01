# Frontend

Angular 22 application for Valorant Tracker. See the [root README](../README.md) for the project overview and the
[backend README](../backend/README.md) for the Spring Boot API.

## Documentation

This README covers setup and commands. The design is documented in [`docs/`](docs):

| Document                             | Covers                                                                        |
| ------------------------------------ | ------------------------------------------------------------------------------ |
| [Architecture](docs/architecture.md) | Directory layout, layering rules, routing, i18n, styling                        |
| [Conventions](docs/conventions.md)   | Naming, signals, templates, accessibility and the rules a reviewer will apply    |
| [Data access](docs/data-access.md)   | `httpResource`, shared vs. parameterized resources, and the state-handling guards |
| [Pages](docs/pages.md)               | What each of the four screens shows, what it fetches and how it behaves          |

Project-wide context is in [`../docs/`](../docs), and the API this module consumes is documented in
[`../backend/docs/api.md`](../backend/docs/api.md).

## Technology stack

- Angular 22, running **zoneless** - `zone.js` is not a dependency
- TypeScript, strict, with `strictTemplates`
- Tailwind CSS v4, configured through CSS `@theme` tokens
- [Lucide](https://lucide.dev/) for icons, via `@lucide/angular`
- Vitest for unit tests, ESLint and Prettier for quality

## Architecture at a glance

The application is organized by **domain**, not by technical type. A feature's model, data access and presentation live
next to each other, so adding one means touching one folder rather than four.

```text
src/
├── app/
│   ├── core/        Domain logic and data access. No components.
│   │   ├── challenges/  players/  matches/  ranking/   One folder per domain
│   │   ├── date/        Calendar-week and local date-time helpers
│   │   ├── http/        Endpoint catalogue, pagination model, resource-state guards
│   │   └── i18n/        Translation service, pipe and title strategy
│   ├── shared/      Reusable presentational components (avatar, pagination, select…)
│   ├── layout/      Application shell (sidebar)
│   └── pages/       Routed screens, each lazy-loaded
├── environments/    Build-time configuration
└── styles/          Tailwind theme tokens
```

Four rules carry the structure. They are conventions enforced by review, not by tooling:

- `core/` never contains components. It holds models, data-access services and pure functions.
- `shared/` contains components reused by more than one page. It may import **types** from `core/`, never services.
- `pages/` compose `core/` and `shared/`. **Pages never import from one another.**
- Anything used by exactly one page stays inside that page's folder.

Cross-folder imports use the aliases `@core/*`, `@shared/*`, `@layout/*`, `@pages/*` and `@env/*`; same-folder imports
stay relative.

Full details, including file-naming conventions and the reasoning behind the zoneless signal architecture, are in
[`docs/architecture.md`](docs/architecture.md) and
[ADR 0010](../docs/adr/0010-signal-based-zoneless-frontend.md).

### Conventions worth knowing up front

- Change detection is **not** declared on components: `OnPush` is the default in Angular v22+.
- State is signal-based; derived state uses `computed()`.
- Every screen renders its loading, error and empty states through `shared/resource-state`.
- `Resource.value()` **throws** in an error state, even with a `defaultValue`. Always read through
  `resourceValue()` or a `hasValue()` guard.

## Development server

```bash
npm start
```

The application is served on `http://localhost:4200/` and reloads on source changes. `proxy.conf.json` forwards `/api`
to `localhost:8080`, so the backend must be running separately (see the [backend README](../backend/README.md)) and no
CORS configuration is required in development.

## Configuration

`src/environments/environment.ts` holds the production configuration; `environment.development.ts` replaces it in the
`development` build configuration through `fileReplacements` in `angular.json`. Both currently set `apiBaseUrl` to the
relative `/api`. Endpoints are declared once in `core/http/api-endpoints.ts` and resolved against it.

## Commands

| Command                | Effect                                                        |
| ---------------------- | ------------------------------------------------------------- |
| `npm start`            | Dev server on `:4200` with the `/api` proxy                    |
| `npm run build`        | Production build into `dist/`, hashed output                   |
| `npm run watch`        | Development build in watch mode                                |
| `npm test`             | Unit tests (Vitest, through the Angular `unit-test` builder)   |
| `npm run lint`         | ESLint over `src/**/*.ts` and `src/**/*.html`                  |
| `npm run format`       | Prettier over `src/**/*.{ts,html,css,scss}`                    |
| `npm run format:check` | Prettier in check mode                                         |

Run `npm run format` and `npm run lint` after every change.

No end-to-end framework is configured, and test coverage is currently thin - one spec, on `Tooltip`. Unlike the
backend, the frontend has no coverage gate and no CI workflow; both are open gaps rather than deliberate omissions.

## Icons

Icons are provided by Lucide. Import each icon's standalone component directly in the consuming component's `imports`
array, then apply it via its selector attribute on an `<svg>` element:

```typescript
import { Component } from '@angular/core';
import { LucideHouse } from '@lucide/angular';

@Component({
  selector: 'app-example',
  imports: [LucideHouse],
  template: `<svg lucideHouse class="h-5 w-5"></svg>`,
})
export class Example {}
```

Size and color icons with Tailwind utilities (`h-*`/`w-*` for size, `text-*` for color via `currentColor`). Do not
register a global icon library unless a demonstrated need arises.

## Design material

- [`docs/images`](docs/images) - UI mockups, the source of truth for each screen. Inspect the relevant mockup before
  implementing or changing a screen.
- [`docs/preview`](docs/preview) - screenshots of the implemented screens, captured from the running application.
  Update the relevant screenshot whenever a previewed screen changes visibly.
- [`docs/boss.md`](docs/boss.md) - specification of a weekly-boss scoring model. A design document for a future
  iteration; no part of it is implemented.

## Code scaffolding

```bash
ng generate component component-name
ng generate --help
```

See the [Angular CLI reference](https://angular.dev/tools/cli) for the full command list.

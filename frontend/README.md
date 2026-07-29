# Frontend

Angular 22 application for Valorant Tracker. See the [root README](../README.md) for the project overview and the
[backend README](../backend/README.md) for the Spring Boot API.

## Technology stack

- Angular 22
- TypeScript
- Tailwind CSS
- [Lucide](https://lucide.dev/) for icons, via `@lucide/angular`

## Architecture

The application is organized by **domain**, not by technical type. A feature's model, data access and
presentation live next to each other, so adding one means touching one folder rather than four.

```text
src/
├── app/
│   ├── core/        Domain logic and data access. No components.
│   │   ├── challenges/  players/  ranking/  matches/   One folder per domain
│   │   ├── date/        Calendar-week helpers
│   │   ├── http/        Endpoint catalogue, pagination model, resource helpers
│   │   └── i18n/        Translation service, pipe and title strategy
│   ├── shared/      Reusable presentational components (avatar, pagination, select…)
│   ├── layout/      Application shell (sidebar)
│   └── pages/       Routed screens, each lazy-loaded
└── environments/    Build-time configuration
```

### Layering rules

- `core/` never contains components. It holds models, data-access services and pure functions.
- `shared/` contains components reused by more than one page. It may import **types** from `core/`,
  never services.
- `pages/` compose `core/` and `shared/`. Pages never import from one another.
- Anything used by exactly one page stays inside that page's folder.

### File naming

The suffix states what a module exports, so the contents are predictable from the file tree:

| Suffix           | Contains                                                         |
| ---------------- | ---------------------------------------------------------------- |
| `*.model.ts`     | Types only — usually mirroring a backend DTO                     |
| `*.utils.ts`     | Pure functions (formatters, resolvers of Tailwind classes, math) |
| `*.constants.ts` | Constant values only                                             |
| `*-api.ts`       | A `@Service` exposing `httpResource`-backed data access          |

Components, pipes and services are named after the class they export (`sidebar.ts`, `players-api.ts`).

### Path aliases

Cross-folder imports use aliases rather than `../../../` chains. Same-folder imports stay relative.

```typescript
import { PlayersApi } from '@core/players/players-api';
import { Avatar } from '@shared/avatar/avatar';
import { environment } from '@env/environment';
```

Available aliases: `@core/*`, `@shared/*`, `@layout/*`, `@pages/*`, `@env/*`.

### Configuration

`src/environments/environment.ts` holds the production configuration; `environment.development.ts`
replaces it in the `development` build configuration via `fileReplacements` in `angular.json`.
Endpoints are declared once in `core/http/api-endpoints.ts` and resolved against `apiBaseUrl`.

### Conventions worth knowing

- Change detection is **not** declared on components: `OnPush` is the default in Angular v22+.
- The application runs **zoneless** — `zone.js` is not a dependency.
- State is signal-based; derived state uses `computed()`.
- Every screen renders its loading, error and empty states through `shared/resource-state`.

## UI mockups

The [`docs/images`](docs/images) directory contains the mockups used as the source of truth for the application's
screens. Inspect the relevant mockup before implementing or changing a screen.

## Previews

The [`docs/preview`](docs/preview) directory contains up-to-date screenshots of the currently implemented screens,
captured from the running application. Update the relevant screenshot whenever a previewed screen changes visibly.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will
automatically reload whenever you modify any of the source files. The backend must be running separately
(see the [backend README](../backend/README.md)) for API calls to succeed.

## Icons

Icons are provided by Lucide. Import each icon's standalone component directly in the consuming component's
`imports` array, then apply it via its selector attribute on an `<svg>` element:

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

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.

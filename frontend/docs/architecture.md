# Frontend architecture

How the Angular application is organized and why. The rationale for the two structural choices is in
[ADR 0010](../../docs/adr/0010-signal-based-zoneless-frontend.md) and
[ADR 0011](../../docs/adr/0011-domain-oriented-frontend.md).

## Shape of the application

A read-only view over the API. Every screen follows the same shape: issue one or more GET requests, show a loading
state, show an error state with a retry, show an empty state, otherwise render. Filters change the request; nothing is
ever written back.

That shape is why the application runs **zoneless** on signals and `httpResource` rather than on `zone.js` and
`Observable` streams - and why there is no form, no store and no interceptor.

## Directory layout

Code is organized by **domain**, not by technical type. A feature's model, data access and formatting helpers live next
to each other.

```text
src/
├── app/
│   ├── core/        Domain logic and data access. No components, ever.
│   │   ├── challenges/  Challenge model, API service, icon and metric resolution
│   │   ├── players/     Player models, API service, tier / avatar / stat formatting
│   │   ├── matches/     Match and season models, API services, game modes, formatting
│   │   ├── ranking/     Ranking models, API service, position styling
│   │   ├── date/        Calendar-week helpers and local date-time formatting
│   │   ├── http/        Endpoint catalogue, pagination model, resource-state guards
│   │   └── i18n/        Translation service, pipe, constants and title strategy
│   ├── shared/      Presentational components reused by more than one page
│   ├── layout/      Application shell (sidebar)
│   ├── pages/       Routed screens, each lazy-loaded
│   ├── app.ts / app.html / app.config.ts / app.routes.ts
├── environments/    Build-time configuration
└── styles/          Tailwind theme tokens (colors, typography)
```

### Layering rules

| Directory   | May contain                                            | May import                       |
| ----------- | ------------------------------------------------------ | -------------------------------- |
| `core/`     | Models, data-access services, pure functions            | `@env/*`, other `core/`          |
| `shared/`   | Presentational components                               | **Types** from `core/`, never services |
| `layout/`   | The application shell                                   | `core/`, `shared/`               |
| `pages/`    | Routed screens and their private sub-components         | `core/`, `shared/`               |

Two rules carry more weight than the rest:

- **Pages never import from one another.** Doing so would silently pull two lazy-loaded route chunks into one bundle.
- **Anything used by exactly one page stays inside that page's folder.** `overview/podium/`,
  `player-profile/match-day.utils.ts` and `players/players.model.ts` are page-private on purpose. A component moves to
  `shared/` when a second page needs it, not before.

These are conventions enforced by review, not by tooling. Nothing prevents a violation at compile time.

### Path aliases

Cross-folder imports use aliases; same-folder imports stay relative.

```typescript
import { PlayersApi } from '@core/players/players-api';
import { Avatar } from '@shared/avatar/avatar';
import { environment } from '@env/environment';
```

Available: `@core/*`, `@shared/*`, `@layout/*`, `@pages/*`, `@env/*`. They are declared in `tsconfig.json`.

## Bootstrap

`app.config.ts` registers everything the application needs, and nothing more:

```typescript
provideBrowserGlobalErrorListeners(),
provideRouter(routes, withComponentInputBinding()),
provideHttpClient(withFetch()),
{ provide: TitleStrategy, useClass: TranslatedTitleStrategy },
provideAppInitializer(() => inject(Translation).initialize()),
```

- `withFetch()` selects the fetch backend over the legacy XHR one, which a zoneless application has no reason to keep.
- `withComponentInputBinding()` binds route parameters straight to component `input()`s, so `players/:id` arrives as
  `PlayerProfile.id` without touching `ActivatedRoute`.
- The app initializer loads the translation dictionary **before** the UI renders, so no screen ever flashes raw
  translation keys.

## Routing

Every route is lazy-loaded through `loadComponent`, so route-level code splitting stays automatic.

| Path         | Screen                                       | Title key             |
| ------------ | -------------------------------------------- | --------------------- |
| `''`         | [Overview](pages.md#overview)                 | `overview.title`      |
| `players`    | [Players](pages.md#players)                   | `players.title`       |
| `players/:id`| [Player profile](pages.md#player-profile)     | `playerProfile.title` |
| `ranking`    | [Ranking history](pages.md#ranking-history)   | `ranking.title`       |
| `**`         | Not found                                     | `notFound.title`      |

A route's `title` is a **translation key**, not a literal. Angular's default strategy would write it verbatim and leave
the browser tab in a single hard-coded language; `TranslatedTitleStrategy` resolves it against the active dictionary and
re-applies it whenever the language changes.

## Layout

`app.html` is the shell: a `h-screen` flex container holding the sidebar and a scrolling `<main>` for routed content.
`html` and `body` are `overflow: hidden` so the page never grows a second browser scrollbar alongside the one `<main>`
already owns.

`Sidebar` renders as a collapsible vertical rail from `lg` up and as a bottom tab bar below it. Its host is
`display: contents` so the inner `<aside>` is itself the flex item of the shell, which is what lets it reorder from
first (rail) to last (tab bar).

Pages share `PAGE_LAYOUT_CLASS` as their host class, so every screen stacks its blocks with the same rhythm. Overview
deliberately opts out: it owns a full-height scroll-snap container instead of a vertical stack.

## Internationalization

French is the default language, English is supported, and the choice persists to `localStorage`.

- `Translation` (`@Service`) owns the active language and the loaded dictionary, both signals. Dictionaries are plain
  JSON files under `public/i18n/`, keyed by dot-separated paths (`sidebar.nav.overview`).
- `TranslatePipe` is deliberately **impure**: the lookup depends on the language signal, not on the arguments Angular
  tracks, so a pure pipe would never re-evaluate on a language switch.
- An effect keeps `<html lang>` in sync with the active language.
- An unknown key resolves to the key itself, so a missing translation is visible rather than blank.

## Styling

Tailwind CSS v4, configured through CSS rather than a JavaScript config file.

- `src/styles/colors.css` declares the palette as `@theme` tokens, extracted from the mockups: a Valorant-red brand
  scale, a dark navy surface scale, text tones, seven category accents, podium colors and semantic states. They surface
  as utilities - `bg-surface-900`, `text-accent-green`, `border-brand-500`.
- `src/styles/typography.css` declares `--font-display` (Rajdhani) for the wordmark.
- `src/styles.css` holds the base layer and two custom utilities, `focus-ring` and `focus-ring-inset`. The user-agent
  focus outline is nearly invisible against `surface-950`, so every interactive element carries one of them explicitly.
  `outline` is used rather than `box-shadow` so the indicator survives inside `overflow-clip` containers and never
  participates in layout.

Icons come from Lucide via `@lucide/angular`. Each icon's standalone component is imported directly by the component
that uses it and applied as an attribute on an `<svg>`; there is no global icon registry. Size and color come from
Tailwind utilities (`h-5 w-5`, `text-accent-green` through `currentColor`).

## Configuration

`src/environments/environment.ts` is the production configuration; `environment.development.ts` replaces it in the
`development` build configuration through `fileReplacements` in `angular.json`.

Both currently set `apiBaseUrl` to the relative `/api`: the backend serves the built frontend from the same origin in
production, and the dev server proxies `/api` to `localhost:8080` through `proxy.conf.json`. Nothing needs CORS in
either case. Point `apiBaseUrl` at an absolute URL when the API is deployed on a different origin.

Endpoints are declared once in `core/http/api-endpoints.ts` and resolved against `apiBaseUrl`, so the REST contract is
described in one place rather than repeated as literals across services.

## Build and quality

| Command                | Effect                                                        |
| ---------------------- | ------------------------------------------------------------- |
| `npm start`            | Dev server on `:4200` with the `/api` proxy                    |
| `npm run build`        | Production build into `dist/`, hashed output                   |
| `npm test`             | Unit tests through the Angular `unit-test` builder (Vitest)    |
| `npm run lint`         | ESLint over `src/**/*.ts` and `src/**/*.html`                  |
| `npm run format`       | Prettier over `src/**/*.{ts,html,css,scss}`                    |
| `npm run format:check` | Prettier in check mode                                         |

TypeScript runs strict, with `strictTemplates`, `typeCheckHostBindings`, `strictInjectionParameters` and
`strictInputAccessModifiers` enabled. The production build enforces a 500 kB initial-bundle warning (1 MB error) and a
4 kB per-component-style warning (8 kB error).

Test coverage is currently thin - one spec, on `Tooltip`. Unlike the backend, the frontend has no coverage gate and no
CI workflow; both are open gaps rather than deliberate omissions.

# ValoQuests — Frontend

Angular single-page application for ValoQuests: the base and its rocket, the ten-planet campaign, the
challenges, the leaderboard, the player profiles and the backoffice. It reads everything from the
[backend API](../backend/README.md).

| | |
|---|---|
| Framework | Angular 22, standalone components, signals, zoneless, no NgModules |
| Language | TypeScript 6, strict |
| Styling | Tailwind 4 plus the "Expédition" design system in `src/styles/` |
| Icons and charts | `@lucide/angular`, `chart.js` (lazy, profile screens only) |
| Tests | Vitest with jsdom |
| Gates | Prettier, ESLint (`angular-eslint`), `ng build` |

## Getting started

Requirements: Node.js 22, and the backend running on `localhost:8080`.

```bash
npm ci
npm start        # dev server on :4200, proxies /api to localhost:8080
```

`proxy.conf.json` keeps both sides same-origin during development, so no CORS setup is needed.
`node_modules/` is not checked in: `npm ci` comes before any lint, test or build.

## Commands

```bash
npm start                                                        # dev server
npm test -- --watch=false                                        # the suite CI runs
npm test -- --include=src/app/core/admin/admin-session.spec.ts   # a single spec
npm run lint
npm run format                                                   # format:check is the CI gate
npm run build                                                    # production bundle in dist/
```

## Structure

Path aliases: `@core/*`, `@shared/*`, `@layout/*`, `@pages/*`, `@env/*`.

| Folder | What lives there |
|---|---|
| `core/` | Data access (`*-api.ts`), models and pure `*.utils.ts` helpers, grouped by domain: admin, campaign, challenges, matches, players, ranking, i18n, http, viewport |
| `pages/` | Routed screens with their own sub-components |
| `layout/` | The `Shell` (sidebar) and the page header |
| `shared/` | Presentational primitives: gauges, tiles, drawers, empty states, the rocket, the charts |
| `styles/` | `colors.css`, `typography.css`, `elevation.css`, `animations.css` |

A screen is `x.ts` + `x.html` + `x.css`, plus `x.model.ts` for its view models.

## Data access

- `@Service()` from `@angular/core`, not `@Injectable`.
- Every backend URL is declared once in `core/http/api-endpoints.ts`. Components never build URLs.
- `*-api.ts` services expose `httpResource<T>()` as fields, so every consumer shares one in-flight
  request instead of triggering its own call.
- **Always read a resource through `resourceValue(resource, fallback)`.** `value()` throws once the
  resource settles into an error state, even when declared with a `defaultValue`, and takes navigation
  down with it.
- Combine multi-resource views with `anyLoading` / `anyError` / `reloadAll`
  (`core/http/resource-state.utils.ts`) and render them through the `ResourceState` component.
- `environment.apiBaseUrl` is relative (`/api`) by default: the app is served from the same origin as
  the API in production. Point it at an absolute URL only if that stops being true.

## Routing

`app.routes.ts` is deliberately mostly **eager**. Public pages ride the initial bundle because
route-level splitting emitted a chunk per shared primitive and cost more requests than it saved. Only
the profile and comparison screens (which own `chart.js`), the tour, the rules and the backoffice are
lazy. The landing page and the tour must stay declared before the `Shell` route: they render
chrome-free.

## Internationalization

French and English dictionaries live in `public/i18n/*.json` and are resolved by `TranslatePipe`.
Route titles are translation keys resolved by `TranslatedTitleStrategy`. **No user-facing literal in a
template**, ever.

## Backoffice

The admin area is reachable by URL only. `adminKeyInterceptor` attaches the `X-Admin-Key` header to
`/api/admin` requests and `adminGuard` protects the routes (`core/admin`).

## Design system

The "Expédition" identity lives in `src/styles/`. The site is **dark only**, and there is no light
theme by design.

- Reuse the existing utilities before writing new class chains: `notch-tr`, `clip-hex`,
  `label-caption`, `menu-panel`, `menu-option`, `ambient-field`.
- Take colors from `colors.css` rather than inventing them.
- Read [`docs/DESIGN.md`](../docs/DESIGN.md) §8 before restyling anything: every constraint listed
  there is a correction already made once.
- Screen redesigns go through a published mockup before any Angular is written.
- Icons are a vocabulary, not decoration: the same Lucide icon means the same thing on every screen,
  and it appears where the word appears, including mid-sentence.

## Code style

TypeScript is strict (`noUnusedLocals`, `noPropertyAccessFromIndexSignature`, `strictTemplates`).
ESLint adds explicit member accessibility, member ordering (fields, constructor, methods), no
`console.log`, `app` selector prefixes, native control flow only (`@if` / `@for`), `NgOptimizedImage`
for static images, and an explicit `type` on every button.

## Docker

`Dockerfile` builds the production bundle on `node:22-alpine` and serves it with nginx
(`nginx.conf`). Quality gates run in CI before an image is built, so the build stage only produces the
artifact. In production Traefik terminates HTTPS and routes `/api/*` to the backend on the same origin.

---

Design direction: [`docs/DESIGN.md`](../docs/DESIGN.md) ·
game rules: [`docs/GAMEPLAY.md`](../docs/GAMEPLAY.md) ·
product overview: [root README](../README.md).

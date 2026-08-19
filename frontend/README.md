# ValoQuests — Frontend

The site people actually look at. Angular 22, standalone, zoneless, and deliberately **incapable of
computing anything**: every number on screen was calculated by the
[backend](../backend/README.md) and is rendered here as-is.

> New here? Start with the [root README](../README.md) — it covers the product and the system-wide
> architecture this document assumes you have already seen.

## Role in the system

A read-only HTTP consumer of `/api/**`, plus a small session-gated backoffice on top of
`/api/admin/**`. That constraint is the whole design brief:

- No duplicated domain logic. A damage formula lives in exactly one place, and it is not here.
- No client-side state machine. Server data *is* the state; the UI is a projection of it.
- The backend must be running and reachable, or every data screen shows its error state.

What the frontend does own: routing, layout, i18n, the design system, loading/error/empty presentation,
and the backoffice's session handling.

## Stack

| Concern | Choice | Notes |
| --- | --- | --- |
| Framework | Angular 22, fully standalone | No `NgModule` anywhere under `src/app` |
| Change detection | Zoneless | `provideHttpClient(withFetch())` — the fetch backend, not the legacy XHR one |
| Server state | Signals + `httpResource` | No NgRx, no Akita, no RxJS store |
| Styling | Tailwind v4, CSS-first | No `tailwind.config.js`; the theme is `@theme` blocks in `src/styles/` |
| Icons | `@lucide/angular` | |
| i18n | Hand-rolled | JSON dictionaries in `public/i18n/`, English and French |
| Tests | Vitest via `@angular/build:unit-test` | No separate `vitest.config.ts` |
| Lint / format | angular-eslint, Prettier | Both enforced in CI |

## Getting it running

```bash
npm install
npm start        # http://localhost:4200
```

`npm start` proxies every `/api/...` request to `http://localhost:8080` (`proxy.conf.json`), so the
dev server and the API stay same-origin and no CORS setup is needed locally. Start the
[backend](../backend/README.md) first or you will get a site full of error states.

## Commands

```bash
npm start            # dev server with proxy and live reload
npm run build        # production build → dist/frontend/browser/
npm run watch        # development build, rebuilt on change
npm test             # Vitest
npm run lint         # angular-eslint
npm run format       # prettier --write
npm run format:check # prettier --check
```

Run `npm run format` and `npm run lint` before pushing — CI runs
`format:check`, `lint`, `test`, `build` in that order
([`frontend-ci.yml`](../.github/workflows/frontend-ci.yml)), and a formatting diff fails the build as
hard as a broken test.

## Project structure

```text
src/
├── app/
│   ├── core/       Data access and domain-adjacent logic. No UI, ever.
│   ├── layout/     Application chrome: shell, sidebar, page header, drawer state
│   ├── pages/      One folder per routed screen
│   └── shared/     Reusable presentational components
├── environments/   Build-time configuration
├── styles/         Design tokens and animations, imported by styles.css
└── styles.css      Tailwind entry point + shape/utility layer
public/i18n/        en.json, fr.json
```

Path aliases (`tsconfig.json`): `@core/*`, `@shared/*`, `@layout/*`, `@pages/*`, `@env/*`. Use them —
relative `../../../` imports across these boundaries will not survive review.

`core/` is organized by backend domain, not by technical kind:

```text
core/
├── http/         api-endpoints.ts, page-response.model.ts, resource-state.utils.ts
├── players/  matches/  challenges/  ranking/  boss/    *-api.ts + *.model.ts + *.utils.ts
├── admin/        session, guard, HTTP interceptor, command runner
├── i18n/         Translation service, TranslatePipe, TranslatedTitleStrategy
├── date/         week-period and countdown helpers
├── landing/  tour/   one-time-entry guards and their visit records
└── snackbar/     the global outcome queue
```

## Routing

Every route in `app.routes.ts` is lazy-loaded via `loadComponent`, so code splitting stays automatic.
Route `title`s are **translation keys**, resolved against the active dictionary by
`TranslatedTitleStrategy` and suffixed with the application name.

The route table has a shape worth understanding before editing it:

```text
''            (pathMatch: 'full')  → Landing      chrome-free doorway, landingEntryGuard
'tour'                             → Tour         chrome-free briefing, tourEntryGuard
'admin/login'                      → AdminLogin   chrome-free sign-in
''                                 → Shell        the sidebar layout
   ├── overview | challenges | leaderboard | players | players/:id | campaign | rules
   ├── admin/operations | admin/players | admin/maintenance | admin/design-system   (adminGuard)
   └── '**'                        → NotFound
```

Two empty paths coexist: the first matches the root URL exactly and serves the landing page;
everything else falls through to the second, which activates `Shell`.

**The three chrome-free routes must stay declared before `Shell`.** Declared after it, they would be
resolved as its children — wrapped in the sidebar, or handed to the wildcard.

**The wildcard is a `Shell` child on purpose**, so a wrong URL still lands on a page the visitor can
navigate away from.

The backoffice is reachable by URL only; nothing in the site links to it. Signing in swaps the
sidebar's entries rather than replacing the layout, so the operator stays in the same application.

### One-time entries

`landingEntryGuard` and `tourEntryGuard` let their page render once per visitor and redirect returning
visitors to `/overview`. Both honour the same `?replay` query parameter — one convention, reused
rather than redeclared — which is what the rules page's "replay the tour" link relies on. The visit
record itself lives in `LandingVisit` / `TourVisit`.

## State management

There is no store. Server data is held by Angular's `httpResource`, and everything else is a plain
signal.

Each domain exposes one `@Service()`-decorated `*-api.ts`:

```ts
// No-parameter resource: created once at service level, so every consumer shares one in-flight
// request instead of each triggering its own GET /api/players.
public readonly players = httpResource<readonly PlayerSummary[]>(
  () => API_ENDPOINTS.players,
  { defaultValue: [] },
);

// Parameterized resource: created per call site, taking reactive Signals as inputs.
public details(
  id: Signal<number>,
  gameMode: Signal<GameMode>,
  seasonId: Signal<number | null>,
  weekStart: Signal<string | null>,
): HttpResourceRef<PlayerDetails | undefined>
```

That split is the rule: **share it at service level if it takes no parameters, expose it as a function
if it does.**

### Reading resources safely

`Resource.value()` **throws** once a resource settles into an error state — even one declared with a
`defaultValue`. An unguarded `value()` in a template breaks navigation when the backend is down.

Always go through `core/http/resource-state.utils.ts`:

| Helper | Use |
| --- | --- |
| `resourceValue(resource, fallback)` | Read a value without throwing. **Use this instead of `value()`.** |
| `anyLoading(...resources)` | Combined loading state — a view is only as ready as its slowest dependency |
| `anyError(...resources)` | Combined error state — one failure is enough to refuse a half-populated screen |
| `reloadAll(...resources)` | Retry everything, since a combined view cannot tell which one failed |

These pair with the shared `<app-resource-state>` component, which renders uniform loading, error and
empty states across every page. Use it rather than hand-rolling a spinner.

## API communication

`core/http/api-endpoints.ts` centralizes **every** backend URL, resolved against
`environment.apiBaseUrl`. Never write an endpoint literal in a service. Public endpoints and
administration endpoints are separated into two groups within that object so nothing outside the
backoffice reaches for an admin route by accident.

Paginated responses arrive in the backend's `PageResponse<T>` envelope, typed in
`core/http/page-response.model.ts`.

## Authentication

There are no user accounts. The backend's single `ADMIN_API_KEY` is the entire credential, so "signed
in" means nothing more than holding a key the API accepts.

```text
AdminSession          Holds the key in sessionStorage. The only place that reads or writes it.
adminKeyInterceptor   Attaches X-Admin-Key to /api/admin requests. Signs out on 401/403.
adminGuard            Redirects to /admin/login when no key is held.
```

Three decisions worth not undoing:

- **`sessionStorage`, not `localStorage`.** The key is readable by any script on the page — acceptable
  for one shared key on a personal project — so it dies with the tab rather than waiting there for the
  next visitor.
- **The interceptor is scoped to `/api/admin`.** The public API needs no credential, and sending one
  everywhere would hand the key to routes that have no business seeing it.
- **A request that already carries the header is left alone, failures included.** That is the sign-in
  probe testing a key the session does not hold yet; signing out over its rejection would be
  meaningless, and the login screen needs the raw error to tell a missing key from a wrong one.

`adminGuard` is **not a security boundary** — the API is. It only spares the operator a screen full of
failed requests.

### Running an admin command

Mutating backoffice actions go through `AdminCommandRunner`, which owns the busy/settled/failed cycle
so no page re-derives its own `try/catch/finally`. The outcome is reported through the global
`Snackbar`, not inline: a fixed bottom-anchored snackbar has one slot, and messages are *queued*
rather than overwritten, because the operations screen runs several commands back to back.

## Application chrome

`Shell` (`layout/shell/`) is the sidebar layout. Its scroll container belongs to the routed content,
which is why `html, body { overflow: hidden }` in `styles.css` is load-bearing rather than cosmetic.

`<app-page-header>` (`layout/page-header/`) is the horizontal bar at the top of the routed content at
every breakpoint — rendered by the page itself, `sticky` inside the shell's scroll container. It takes
three inputs (`eyebrow`, `heading`, `backLink`) and two projection slots (`[headingAside]`, and the
default one for the page's own countdown, action or view toggle).

**Every page nested under `Shell` must render it as its first child.** Below `lg` the sidebar is a
drawer with no bar of its own, and this component carries the burger that opens it — a page without it
is a phone with no way into the navigation. The shared open state lives in `layout/navigation-panel.ts`.

Detail pages pass `backLink` (which turns the eyebrow line into the way back to the parent) and drop
`heading` when their own opening block already names the subject.

`PAGE_LAYOUT_CLASS` → the `page-stack` utility stays bare so the bar reaches the edges of the column
`main` gives it; the gutter and the 1440 px cap ride on the page's *blocks* instead. A block that hosts
itself as `display: contents` slips through that rule and comes out flush, which is why
`app-resource-state` and `app-rule-section` carry a box of their own.

## Styling

Tailwind v4, configured in CSS. `src/styles.css` imports the token files and declares the project's
own utility layer:

| File | Holds |
| --- | --- |
| `src/styles/colors.css` | `@theme` — brand, surfaces, text, category accents, podium, semantic success/danger |
| `src/styles/typography.css` | `@theme` — the type scale |
| `src/styles/animations.css` | Keyframes and motion utilities |
| `src/styles.css` | Tailwind entry, base layer, and the shape/run utilities below |

Reach for these before writing a class run by hand — **each one exists because the hand-written
version had already drifted between screens**:

- `notch-tr` / `notch-tr-edge` / `clip-hex` / `clip-shear` — the direction's silhouettes
- `label-caption` — the mono uppercase micro-label captioning a value (70+ call sites; carries the
  typography, never a color)
- `menu-panel` / `menu-option` — the surface and rows of any panel opening over the page
- `ambient-field`, `scroll-subtle`
- `focus-ring` / `focus-ring-inset` — custom `@utility` classes replacing the default outline, which is
  invisible against `surface-950`

**`/admin/design-system` is the live catalogue** of every token and shared component, rendered against
fixed mock data. Add a section to it when you add a shared primitive. Its comments also record the
patterns that deliberately have *no* component — panels, cards, grid-based tables, background
treatments — which is worth reading before you invent one.

## Internationalization

Hand-rolled, not a library. `Translation` (`core/i18n/translation.ts`) loads
`public/i18n/{lang}.json`, exposes `translate(key, params?)` and a `language` signal, and persists the
choice to `localStorage`. `provideAppInitializer` loads the initial dictionary before the UI renders,
so no screen ever flashes raw keys.

Templates use `TranslatePipe`:

```html
{{ 'landing.weekLabel' | translate: { week: week } }}
```

Adding user-facing copy means adding the key to **both** `en.json` and `fr.json`. A key present in one
and missing from the other is a bug that only shows up for half the audience.

## Environment configuration

No `.env`. Two files under `src/environments/`, swapped by the `fileReplacements` entry in
`angular.json`:

| File | Used by | `apiBaseUrl` |
| --- | --- | --- |
| `environment.development.ts` | `npm start`, `npm run watch` | `/api` — proxied to `localhost:8080` |
| `environment.ts` | `npm run build` (default configuration) | `/api` — same origin as the deployed site |

Both are relative on purpose: the backend is expected to serve the built frontend from the same
origin. Point `apiBaseUrl` at an absolute URL only if you deploy the API on a different origin — and
then set `FRONTEND_ORIGIN` on the backend to match, or CORS will reject you.

## Build

```bash
npm run build     # → dist/frontend/browser/
```

Builder: `@angular/build:application`. The production configuration adds output hashing and size
budgets — **500 kB warning / 1 MB error** on the initial bundle, 4 kB / 8 kB per component stylesheet.
A budget warning on a routine change usually means an eagerly imported dependency that should have
been lazy.

`public/` is copied verbatim into the output, which is how the i18n dictionaries ship.

## Testing

Vitest through Angular's `@angular/build:unit-test` builder — there is no `vitest.config.ts`. Specs
are colocated with the file under test (`tooltip.ts` + `tooltip.spec.ts`).

```bash
npm test
npm test -- --watch=false   # what CI runs
```

> **Known gap, not a design choice.** Coverage is thin: five spec files today, concentrated on the
> admin session, the HTTP interceptor, the command runner and the resource-state helpers — the pieces
> whose failure modes are silent. Pages and shared components are untested. New logic in `core/`
> should arrive with a spec.
>
> Single-file filtering through the CLI has not been confirmed against the installed `@angular/build`
> version; check before relying on a specific flag.

## Conventions

Enforced by `eslint.config.js` — these fail `npm run lint`, not review:

- Components: element selector, `app-` prefix, kebab-case. Directives: attribute selector, `app`
  prefix, camelCase.
- **Explicit member accessibility on every class member** (constructors excluded).
- Member order: `signature, field, constructor, method` — deliberately *not* accessibility-ordered, so
  `inject()`-initialized fields precede the derived public signals that read them.
- Templates: native control flow only (`@if` / `@for`) — no `*ngIf` / `*ngFor`. Static images through
  `NgOptimizedImage`. `eqeqeq`. No positive `tabindex`.
- `no-console` except `console.error` / `console.warn`.
- Prettier: `printWidth: 100`, single quotes, HTML parsed with the `angular` parser.
- `strictTemplates` is on. A template type error is a build failure.

## Extending it

| You want to… | Do this |
| --- | --- |
| Add a screen | Add a lazy `loadComponent` route (as a `Shell` child unless it must be chrome-free), a `title` translation key in both dictionaries, and render `<app-page-header>` first |
| Consume a new endpoint | Add it to `API_ENDPOINTS`, then expose an `httpResource` from the domain's `*-api.ts` — shared if parameterless, a function if not |
| Add a shared component | Put it in `shared/`, then add a section to `/admin/design-system` |
| Add a color, size or motion | Extend the `@theme` block in `src/styles/`, never a one-off hex in a template |
| Add an admin action | Route it through `AdminCommandRunner`; report the outcome through `Snackbar` |
| Add copy | Both `en.json` and `fr.json` |

## Troubleshooting

| Symptom | Cause / fix |
| --- | --- |
| Every screen shows its error state | The backend is not running on `localhost:8080` |
| A screen breaks on navigation instead of showing an error | Somewhere a template calls `value()` directly. Use `resourceValue()` |
| Raw translation keys on screen | The key is missing from the active dictionary |
| A page has no burger below `lg` | It is not rendering `<app-page-header>` as its first child |
| `npm start` fails immediately | `npm install` first |
| CI fails on `format:check` but the app works | Run `npm run format` |
| Production build warns about bundle size | Expected at the 500 kB threshold — investigate if it moved on a change that should not have added weight |
| Redirected to `/overview` when opening `/` or `/tour` | The one-time-entry guards. Append `?replay` |

## Related documents

- [Root README](../README.md) — the product and the system-wide architecture.
- [backend/README.md](../backend/README.md) — the API this site consumes, and its Swagger contract.

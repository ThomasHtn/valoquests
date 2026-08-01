# Frontend conventions

The rules a reviewer will apply. They are conventions, not compile-time constraints, so they are written down rather
than assumed.

## File naming

A file's suffix states what it exports, so contents are predictable from the tree alone.

| Suffix           | Exports                                                        | Example                        |
| ---------------- | -------------------------------------------------------------- | ------------------------------ |
| `*.model.ts`     | Types only, usually mirroring a backend DTO                     | `player-summary.model.ts`      |
| `*.utils.ts`     | Pure functions: formatters, class resolvers, math               | `player-format.utils.ts`       |
| `*.constants.ts` | Constant values only                                            | `translation.constants.ts`     |
| `*-api.ts`       | A `@Service` exposing `httpResource`-backed data access          | `players-api.ts`               |

Components, pipes and services are named after the class they export: `sidebar.ts` exports `Sidebar`,
`players-api.ts` exports `PlayersApi`. External templates and styles sit beside their component and are referenced
relatively.

## Components

- **Standalone by default.** Never set `standalone: true` - it is the default.
- **Never declare `changeDetection`.** `OnPush` is the default in Angular v22+.
- Use `input()` / `output()` functions, never the `@Input` / `@Output` decorators.
- Put host bindings in the `host` object of the decorator, never in `@HostBinding` / `@HostListener`.
- Prefer inline templates for small components. `PointsBadge` is a one-line template on purpose.
- Keep a component focused on one responsibility. A page that grew a distinct block extracts it into a page-private
  sub-component - `overview/podium/`, `overview/team-progress/`.

### Host classes as the component's own box

Several shared components make the **host element** the thing itself rather than wrapping it in a div:

- `ProgressBar`'s host *is* the track; callers size it with a plain `class` attribute (`class="w-16"`, `class="flex-1"`)
  because the width varies with the surrounding layout.
- `PointsBadge`'s host *is* the badge.
- `Avatar`, `PositionBadge`, `RankIconView` and `ResourceState` use `class: 'contents'`, so the host disappears from
  layout and the projected content participates directly in the parent's grid or flex context.

## State

- Local state is a `signal()`; derived state is a `computed()`. Never `mutate` - use `set` or `update`.
- A value that has a computed default but must survive an explicit user choice is a `linkedSignal`, not a signal seeded
  from an effect. The season filter on the player profile is the reference example, and its Javadoc explains why the
  "has the source loaded yet" guard is needed.
- Effects are for synchronizing with something outside Angular (the `<html lang>` attribute, an
  `IntersectionObserver`), not for deriving state.
- RxJS is used where it is genuinely the right tool and nowhere else. The overview's countdown is `interval` with
  `takeUntilDestroyed`; there is no other stream in the application.

## Services

- Prefer `@Service` over `@Injectable({ providedIn: 'root' })` for singletons.
- Inject with `inject()`, not constructor parameters.
- One responsibility per service. Data access per domain (`PlayersApi`, `MatchesApi`, `SeasonsApi`, `RankingApi`,
  `ChallengesApi`) plus `Translation` is the entire service surface.

## Templates

- Native control flow only: `@if`, `@for`, `@switch`. Never `*ngIf`, `*ngFor`, `*ngSwitch`.
- Use `class` and `style` bindings, never `ngClass` or `ngStyle`.
- Keep logic out of templates. A template calls a `computed()` or a function the component exposed; it does not compute.
  Pages expose formatters as fields (`protected readonly formatKda = formatKda`) precisely so templates stay
  declarative.
- Do not assume globals such as `new Date()` are available in a template.
- Use `NgOptimizedImage` for static images. It does not work for inline base64.

## View models

Pages map API DTOs into display-ready row types before rendering - `PlayerRow`, `RankingHistoryWeekView`,
`ChallengeCard`. The mapping resolves avatars, icons, translated labels and formatted numbers once, in a `computed()`,
rather than letting the template do it per row on every change detection pass.

The view model lives in the page's own `*.model.ts`, because it exists for that screen only.

## TypeScript

- Strict type checking. Prefer inference when the type is obvious.
- Avoid `any`; use `unknown` when a type is genuinely uncertain.
- Prefer `readonly` on arrays and interface members that are not reassigned.
- Derive a union from a runtime list rather than declaring both - `GAME_MODES` is a `const` array and `GameMode` is
  `(typeof GAME_MODES)[number]`, which keeps the type and the values in sync by construction.

## Accessibility

Non-negotiable: the UI must pass AXE checks and meet WCAG AA, including focus management, color contrast and ARIA.

Patterns the codebase already settled:

- **Every interactive element carries `focus-ring` or `focus-ring-inset`.** The user-agent outline is invisible on the
  dark ground.
- **A redundant visual encoding is hidden from assistive technology.** `ProgressBar` and `ProgressCircle` are
  `aria-hidden` because every call site renders the same value as adjacent text; exposing them as `progressbar` would
  make a screen reader announce the number twice. A future call site that renders a bar *without* an adjacent value
  must expose the value itself.
- **A decorative image gets an empty `alt`.** `Avatar` renders the player's name as text beside the portrait, so
  announcing it twice would be noise.
- **A disclosure keeps its panel in the DOM.** `CollapsibleCard` and `Select` hide their panel with the `hidden`
  attribute rather than removing it with `@if`, because `aria-controls` pointing at a missing id is a violation - and
  because the loaded content survives a collapse/expand cycle.
- **Per-instance ids come from a module-level counter.** `aria-controls`, `aria-describedby` and
  `aria-activedescendant` must each resolve to exactly one element in the document, so `CollapsibleCard`, `Select` and
  `Tooltip` each own a counter.
- **`Select` implements the ARIA select-only combobox pattern**: the trigger keeps DOM focus and points at the
  highlighted option through `aria-activedescendant`, so arrows, Home/End, Enter, Space and Escape all work.
- **A tooltip is reachable by keyboard**, not only on hover.

## Documentation

Every exported type, service, component member and function carries a TSDoc block beginning with `/**`. The bar is not
"describe the signature" - it is "explain what the reader cannot infer".

The comments worth writing are the ones that record a decision or a trap:

- why `TranslatePipe` is impure;
- why the season filter is a `linkedSignal` and what the `previous.source.length > 0` guard distinguishes;
- why `PositionBadge` draws an SVG polygon rather than applying a CSS `clip-path`;
- why the game-mode filter offers only import-eligible modes;
- why `resourceValue` exists at all.

Do not document the obvious. `/** The player's id. */` above `id` earns nothing.

## Reviewer checklist

- [ ] Does a page import another page? (never allowed)
- [ ] Does a `shared/` component inject a service? (never allowed)
- [ ] Does `core/` contain a component? (never allowed)
- [ ] Is something used by exactly one page sitting in `shared/`?
- [ ] Is `resource.value()` called without a `hasValue()` guard or `resourceValue()`?
- [ ] Does a new screen render all four states through `ResourceState`?
- [ ] Is a new interactive element missing a focus ring?
- [ ] Is a hard-coded string missing from the `fr` and `en` dictionaries?
- [ ] Has `npm run format` and `npm run lint` been run?

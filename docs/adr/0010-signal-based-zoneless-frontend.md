# 0010. Build the frontend zoneless on signals and `httpResource`

## Status

Accepted

## Context

The frontend is a read-only view over the API. Every screen follows the same shape: issue one or more GET requests,
show a loading state, show an error state with a retry, show an empty state, otherwise render. Filters change the
request; nothing is ever written back.

The classic Angular answer - `zone.js` change detection, services returning `Observable`, `async` pipes, manual
subscription bookkeeping - solves a harder problem than this application has, and pays for it in bundle size and in
change-detection cycles triggered by every timer and every DOM event.

## Decision

The application runs **zoneless**: `zone.js` is not a dependency. State is signal-based, derived state uses
`computed()`, and data access is `httpResource`.

- Components declare no `changeDetection`; `OnPush` is the default in Angular v22+.
- `@Service` replaces `@Injectable({ providedIn: 'root' })` for singleton services.
- Resources shared across the application - every player, every season, the current challenges, the current ranking -
  are exposed as fields on their data-access service, so all consumers read the same in-flight request rather than each
  triggering its own.
- Resources parameterized by a caller (one player's details, one page of match history) are factory methods taking
  `Signal` arguments, so changing a filter re-issues the request without any subscription management.
- `withComponentInputBinding()` binds route parameters straight to component `input()`s.
- RxJS survives where it is genuinely the right tool: the overview's countdown uses `interval` with
  `takeUntilDestroyed`.

Two guards are shared rather than reimplemented per screen, because both concern a trap in the resource API:

- `resourceValue(resource, fallback)` - `Resource.value()` **throws** once a resource settles into an error state, even
  when a `defaultValue` was declared. Every computed or template expression reading a resource goes through this guard,
  so a failed request degrades to a fallback instead of breaking navigation.
- `anyLoading(...)` / `anyError(...)` / `reloadAll(...)` - a view depending on several resources is only as healthy as
  its least healthy dependency, and must retry all of them since it cannot tell which one failed.

## Consequences

- No `zone.js` in the bundle, and change detection runs on signal updates rather than on every asynchronous event.
- Loading, error and empty states are declarative. `ResourceState` renders all four states for every screen, so they
  cannot drift apart.
- Every consumer of a shared resource sees the same data at the same time, and a screen combining three of them issues
  three requests rather than three per component.
- The team must know that `value()` throws on error. It is the one sharp edge of this approach, and it is why the guard
  exists and is documented at its definition.
- Zoneless requires that nothing mutates state outside Angular's awareness. Third-party libraries that assume `zone.js`
  cannot be adopted without checking this first.
- Should the application ever need to write, this decision does not block it - but forms would be Signal Forms
  (`@angular/forms/signals`), not the reactive-forms model the rest of the ecosystem still defaults to.

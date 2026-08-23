import { DestroyRef, Injectable, inject, signal, Signal } from '@angular/core';

/**
 * Tailwind's default `lg` and `xl` breakpoints, in pixels — the widths at which the screens
 * holding two distinct layouts switch from the narrow one to the wide one.
 */
const LG_BREAKPOINT_PX = 1024;
const XL_BREAKPOINT_PX = 1280;

/**
 * Reactive viewport width, exposed as the breakpoints the layouts actually branch on.
 *
 * A screen offering a wide layout *and* a narrow one used to render both and hide one with
 * `hidden xl:flex` / `xl:hidden`. That doubles the node count of the heaviest pages, which the
 * browser then styles and lays out for nothing — and the DOM size is what weighs the most in the
 * page's environmental footprint (EcoIndex weights it three times as much as its bytes). Branching
 * on this signal with `@if` keeps a single layout in the DOM, and `matchMedia` re-renders the other
 * one when the window crosses the breakpoint.
 */
@Injectable({ providedIn: 'root' })
export class Breakpoint {
  private readonly destroyRef = inject(DestroyRef);

  /**
   * Whether the viewport is at least Tailwind's `lg` breakpoint (1024px).
   */
  public readonly isLarge: Signal<boolean> = this.track(LG_BREAKPOINT_PX);

  /**
   * Whether the viewport is at least Tailwind's `xl` breakpoint (1280px).
   */
  public readonly isWide: Signal<boolean> = this.track(XL_BREAKPOINT_PX);

  /**
   * Tracks one `min-width` media query as a signal, defaulting to the wide layout when the
   * platform has no `matchMedia` at all.
   */
  private track(minWidthPx: number): Signal<boolean> {
    const state = signal(true);

    if (typeof window === 'undefined' || !window.matchMedia) {
      return state.asReadonly();
    }

    const query = window.matchMedia(`(min-width: ${minWidthPx}px)`);
    state.set(query.matches);

    const onChange = (event: MediaQueryListEvent): void => state.set(event.matches);
    query.addEventListener('change', onChange);
    this.destroyRef.onDestroy(() => query.removeEventListener('change', onChange));

    return state.asReadonly();
  }
}

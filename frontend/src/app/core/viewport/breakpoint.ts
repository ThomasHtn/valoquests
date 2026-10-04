import { DestroyRef, Service, inject, signal, Signal } from '@angular/core';
import { MD_BREAKPOINT_PX, LG_BREAKPOINT_PX, XL_BREAKPOINT_PX } from './breakpoint.constants';

/**
 * Viewport breakpoints as signals, so `@if` keeps one layout in the DOM instead of hiding one.
 */
@Service()
export class Breakpoint {
  /**
   * Destroy hook, to detach the media query listeners.
   */
  private readonly destroyRef = inject(DestroyRef);

  /**
   * Whether the viewport is at least `md` (768px).
   */
  public readonly isMedium: Signal<boolean> = this.track(MD_BREAKPOINT_PX);

  /**
   * Whether the viewport is at least `lg` (1024px).
   */
  public readonly isLarge: Signal<boolean> = this.track(LG_BREAKPOINT_PX);

  /**
   * Whether the viewport is at least `xl` (1280px).
   */
  public readonly isWide: Signal<boolean> = this.track(XL_BREAKPOINT_PX);

  /**
   * `min-width` query as a signal, wide when `matchMedia` is missing.
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

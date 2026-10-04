import { DestroyRef, Directive, ElementRef, inject, signal } from '@angular/core';

import { IN_VIEW_ROOT_MARGIN } from './in-view.constants';

/**
 * Holds the host's CSS animations until it first scrolls into view (`.fx-await`), never replayed.
 * Otherwise gauges below the fold finish before anyone reaches them.
 */
@Directive({
  selector: '[appInView]',
  host: { class: 'fx-await', '[class.in-view]': 'seen()' },
})
export class InView {
  /**
   * Whether the host has been on screen once.
   */
  protected readonly seen = signal(typeof IntersectionObserver === 'undefined');

  constructor() {
    if (this.seen()) {
      return;
    }
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          this.seen.set(true);
          observer.disconnect();
        }
      },
      { rootMargin: IN_VIEW_ROOT_MARGIN },
    );
    observer.observe(inject(ElementRef<HTMLElement>).nativeElement);
    inject(DestroyRef).onDestroy(() => observer.disconnect());
  }
}

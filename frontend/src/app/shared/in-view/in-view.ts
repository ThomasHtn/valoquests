import { DestroyRef, Directive, ElementRef, inject, signal } from '@angular/core';

import { IN_VIEW_ROOT_MARGIN } from './in-view.constants';

/**
 * Holds every CSS animation of its host, and of what the host contains, until the host first
 * scrolls into view (see `.fx-await` in `styles/animations.css`).
 *
 * A gauge filling or a ring closing is the moment a figure is read; played below the fold at page
 * load, it has already finished by the time anyone reaches it. Seen once, the host stays released:
 * scrolling back up does not replay anything.
 */
@Directive({
  selector: '[appInView]',
  host: { class: 'fx-await', '[class.in-view]': 'seen()' },
})
export class InView {
  /**
   * Whether the host has been on screen at least once.
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

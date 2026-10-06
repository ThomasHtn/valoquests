import { DOCUMENT, inject, Service, signal } from '@angular/core';
import { NavigationEnd, NavigationStart, Router } from '@angular/router';

import {
  PAGE_BODY_SELECTOR,
  SCROLL_RESTORE_MAX_FRAMES,
  SCROLL_RESTORE_STEP_MS,
  SCROLL_TOP_THRESHOLD_SCREENS,
} from './page-scroll.constants';

/**
 * Per-URL scroll restoration of the page body, which the router's (window-only) one cannot see.
 */
@Service()
export class PageScroll {
  /**
   * Document the page body is looked up and listened to in.
   */
  private readonly document = inject(DOCUMENT);

  /**
   * Router, to key offsets by URL and detect back or forward navigations.
   */
  private readonly router = inject(Router);

  /**
   * Last offset of every visited URL.
   */
  private readonly offsets = new Map<string, number>();

  /**
   * Whether the navigation in flight is a browser back or forward.
   */
  private restoring = false;

  /**
   * Timer of the restore attempt in flight, cancelled when another navigation starts.
   */
  private restoreTimer: ReturnType<typeof setTimeout> | undefined;

  /**
   * Whether the page is scrolled deep enough to offer back-to-top.
   */
  public readonly deep = signal(false);

  constructor() {
    // Scroll events do not bubble, so listen in the capture phase.
    this.document.addEventListener('scroll', (event) => this.onScroll(event), {
      capture: true,
      passive: true,
    });

    this.router.events.subscribe((event) => {
      if (event instanceof NavigationStart) {
        clearTimeout(this.restoreTimer);
        this.restoring = event.navigationTrigger === 'popstate';
      } else if (event instanceof NavigationEnd) {
        // Read the body rather than reset: a query-only navigation keeps the same scrolled page.
        const body = this.body();
        this.deep.set(!!body && body.scrollTop > body.clientHeight * SCROLL_TOP_THRESHOLD_SCREENS);
        if (this.restoring) {
          this.restore(this.offsets.get(event.urlAfterRedirects) ?? 0);
        }
      }
    });
  }

  /**
   * Scrolls to the top, smoothly unless reduced motion is asked.
   */
  public toTop(): void {
    const reduced = this.document.defaultView?.matchMedia('(prefers-reduced-motion: reduce)');
    this.body()?.scrollTo({ top: 0, behavior: reduced?.matches ? 'auto' : 'smooth' });
  }

  /**
   * Records the page body's offset for the current URL and updates the back-to-top state.
   */
  private onScroll(event: Event): void {
    const target = event.target;
    if (!(target instanceof HTMLElement) || !target.matches(PAGE_BODY_SELECTOR)) {
      return;
    }
    this.offsets.set(this.router.url, target.scrollTop);
    this.deep.set(target.scrollTop > target.clientHeight * SCROLL_TOP_THRESHOLD_SCREENS);
  }

  /**
   * Restores an offset once the page is tall enough, which a lazy route may take frames to be.
   */
  private restore(offset: number, frame = 0): void {
    if (offset <= 0) {
      return;
    }
    const body = this.body();
    const reachable = body !== null && body.scrollHeight - body.clientHeight >= offset;
    if (reachable || frame >= SCROLL_RESTORE_MAX_FRAMES) {
      // A route without a page body gives up once the frames run out.
      if (body) {
        body.scrollTop = offset;
      }
      return;
    }
    // Not `requestAnimationFrame`, which a background tab stops firing.
    this.restoreTimer = setTimeout(() => this.restore(offset, frame + 1), SCROLL_RESTORE_STEP_MS);
  }

  /**
   * Scrolling page body of the current route, `null` when none is rendered.
   */
  private body(): HTMLElement | null {
    return this.document.querySelector<HTMLElement>(PAGE_BODY_SELECTOR);
  }
}

import { DOCUMENT, inject, Service, signal } from '@angular/core';
import { NavigationEnd, NavigationStart, Router } from '@angular/router';

import {
  PAGE_BODY_SELECTOR,
  SCROLL_RESTORE_MAX_FRAMES,
  SCROLL_RESTORE_STEP_MS,
  SCROLL_TOP_THRESHOLD_SCREENS,
} from './page-scroll.constants';

/**
 * Scroll behaviour of the routed page's body, which the router's own scroll restoration cannot see
 * since it only knows the window.
 *
 * Remembers each URL's offset so the back button lands where the reader left (a profile opened
 * from the squad list returns to the same row), and exposes whether the reader is deep enough in a
 * page to be offered the way back to the top.
 */
@Service()
export class PageScroll {
  private readonly document = inject(DOCUMENT);
  private readonly router = inject(Router);

  /**
   * Last known offset of every visited URL.
   */
  private readonly offsets = new Map<string, number>();

  /**
   * Whether the navigation in flight was triggered by the browser's back or forward buttons.
   */
  private restoring = false;

  /**
   * Whether the current page is scrolled far enough to offer a way back to the top.
   */
  public readonly deep = signal(false);

  constructor() {
    // Scroll events do not bubble, so the capture phase is the only place to hear every page body.
    this.document.addEventListener('scroll', (event) => this.onScroll(event), {
      capture: true,
      passive: true,
    });

    this.router.events.subscribe((event) => {
      if (event instanceof NavigationStart) {
        this.restoring = event.navigationTrigger === 'popstate';
      } else if (event instanceof NavigationEnd) {
        this.deep.set(false);
        if (this.restoring) {
          this.restore(this.offsets.get(event.urlAfterRedirects) ?? 0);
        }
      }
    });
  }

  /**
   * Scrolls the current page back to its top, smoothly unless the reader asked for less motion.
   */
  public toTop(): void {
    const reduced = this.document.defaultView?.matchMedia('(prefers-reduced-motion: reduce)');
    this.body()?.scrollTo({ top: 0, behavior: reduced?.matches ? 'auto' : 'smooth' });
  }

  private onScroll(event: Event): void {
    const target = event.target;
    if (!(target instanceof HTMLElement) || !target.matches(PAGE_BODY_SELECTOR)) {
      return;
    }
    this.offsets.set(this.router.url, target.scrollTop);
    this.deep.set(target.scrollTop > target.clientHeight * SCROLL_TOP_THRESHOLD_SCREENS);
  }

  /**
   * Restores an offset once the page is tall enough to hold it: content fed by shared resources
   * renders within a frame or two, but a lazy route may take a few more.
   */
  private restore(offset: number, frame = 0): void {
    if (offset <= 0) {
      return;
    }
    const body = this.body();
    const reachable = body !== null && body.scrollHeight - body.clientHeight >= offset;
    if (body && (reachable || frame >= SCROLL_RESTORE_MAX_FRAMES)) {
      body.scrollTop = offset;
      return;
    }
    // A timer rather than `requestAnimationFrame`, which a backgrounded tab stops firing altogether.
    setTimeout(() => this.restore(offset, frame + 1), SCROLL_RESTORE_STEP_MS);
  }

  private body(): HTMLElement | null {
    return this.document.querySelector<HTMLElement>(PAGE_BODY_SELECTOR);
  }
}

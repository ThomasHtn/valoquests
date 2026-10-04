import { inject, Service, signal } from '@angular/core';
import { NavigationEnd, NavigationStart, Router } from '@angular/router';

import { sameRoutePath } from './navigation-history.utils';

/**
 * In-app history, so a back link returns where the reader came from.
 * A forward jump it cannot follow resets it, so a back link falls back to its static parent.
 */
@Service()
export class NavigationHistory {
  /**
   * Router whose navigation events feed the history.
   */
  private readonly router = inject(Router);

  /**
   * In-app URLs up to the current one, oldest first.
   */
  private readonly stack: string[] = [];

  /**
   * Whether the navigation in flight is a browser back or forward.
   */
  private popping = false;

  /**
   * Whether the navigation in flight replaces the current entry.
   */
  private replacing = false;

  /**
   * URL of the previous in-app page, `null` on the first page.
   */
  public readonly previousUrl = signal<string | null>(null);

  constructor() {
    this.router.events.subscribe((event) => {
      if (event instanceof NavigationStart) {
        this.popping = event.navigationTrigger === 'popstate';
        this.replacing = this.router.currentNavigation()?.extras.replaceUrl === true;
      } else if (event instanceof NavigationEnd) {
        this.record(event.urlAfterRedirects);
      }
    });
  }

  /**
   * Updates the stack after a navigation: pops on back or forward, rewrites on a replace, pushes otherwise.
   */
  private record(url: string): void {
    const top = this.stack.at(-1);
    if (this.popping) {
      this.stack.pop();
      if (this.stack.at(-1) !== url) {
        this.stack.splice(0, this.stack.length, url);
      }
    } else if (top !== undefined && (this.replacing || sameRoutePath(top, url))) {
      // A query-only change (a filter in the address) rewrites the entry.
      this.stack[this.stack.length - 1] = url;
    } else {
      this.stack.push(url);
    }
    this.previousUrl.set(this.stack.at(-2) ?? null);
  }
}

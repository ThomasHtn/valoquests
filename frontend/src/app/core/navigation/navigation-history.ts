import { inject, Service, signal } from '@angular/core';
import { NavigationEnd, NavigationStart, Router } from '@angular/router';

import { sameRoutePath } from './navigation-history.utils';

/**
 * The in-app history the browser keeps but does not expose: which page the reader came from.
 *
 * Lets a back link return where the reader really was (a profile opened from the leaderboard goes
 * back to the leaderboard, filters and scroll intact) instead of to a fixed parent. Popstates are
 * matched against the stack; a forward jump it cannot follow resets it, so a back link then falls
 * back to its static parent rather than to a wrong page.
 */
@Service()
export class NavigationHistory {
  private readonly router = inject(Router);

  /**
   * URLs of the in-app entries up to the current one, oldest first.
   */
  private readonly stack: string[] = [];

  /**
   * Whether the navigation in flight comes from the browser's back or forward buttons.
   */
  private popping = false;

  /**
   * Whether the navigation in flight replaces the current entry rather than adding one.
   */
  private replacing = false;

  /**
   * The URL of the in-app page before the current one, or `null` on the first page of the visit.
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

  private record(url: string): void {
    const top = this.stack.at(-1);
    if (this.popping) {
      this.stack.pop();
      if (this.stack.at(-1) !== url) {
        this.stack.splice(0, this.stack.length, url);
      }
    } else if (top !== undefined && (this.replacing || sameRoutePath(top, url))) {
      // A query-only change (a filter kept in the address) rewrites the entry rather than adding one.
      this.stack[this.stack.length - 1] = url;
    } else {
      this.stack.push(url);
    }
    this.previousUrl.set(this.stack.at(-2) ?? null);
  }
}

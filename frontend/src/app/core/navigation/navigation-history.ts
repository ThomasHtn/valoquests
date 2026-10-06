import { inject, Service, signal } from '@angular/core';
import { NavigationEnd, NavigationStart, Router } from '@angular/router';

import { HistoryEntry } from './navigation-history.model';
import { sameRoutePath } from './navigation-history.utils';

/**
 * In-app history, so a back link returns where the reader came from.
 * Entries are matched by the navigation id the router stores in each, so back and forward both land right.
 */
@Service()
export class NavigationHistory {
  /**
   * Router whose navigation events feed the history.
   */
  private readonly router = inject(Router);

  /**
   * In-app entries, oldest first, forward ones included.
   */
  private readonly entries: HistoryEntry[] = [];

  /**
   * Index of the current entry in {@link entries}, `-1` before the first navigation.
   */
  private cursor = -1;

  /**
   * Whether the navigation in flight is a browser back or forward.
   */
  private popping = false;

  /**
   * Navigation id stored in the entry a back or forward returns to, `null` when it has none.
   */
  private restoredId: number | null = null;

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
        this.restoredId = event.restoredState?.navigationId ?? null;
        this.replacing = this.router.currentNavigation()?.extras.replaceUrl === true;
      } else if (event instanceof NavigationEnd) {
        this.record(event.id, event.urlAfterRedirects);
      }
    });
  }

  /**
   * Moves the cursor on back or forward, rewrites on a replace, pushes otherwise.
   */
  private record(id: number, url: string): void {
    if (this.popping) {
      this.restore(id, url);
    } else {
      const current = this.entries[this.cursor];
      if (current !== undefined && (this.replacing || sameRoutePath(current.url, url))) {
        // A query-only change (a filter in the address) rewrites the entry.
        this.entries[this.cursor] = { ids: [...current.ids, id], url };
      } else {
        // As in the browser, a new page drops the entries ahead.
        this.cursor++;
        this.entries.splice(this.cursor, Infinity, { ids: [id], url });
      }
    }
    this.previousUrl.set(this.entries[this.cursor - 1]?.url ?? null);
  }

  /**
   * Points the cursor at the entry a back or forward returned to; an unknown one starts afresh.
   */
  private restore(id: number, url: string): void {
    const restoredId = this.restoredId;
    const index =
      restoredId === null ? -1 : this.entries.findIndex((entry) => entry.ids.includes(restoredId));
    if (index === -1) {
      this.entries.splice(0, Infinity, { ids: restoredId === null ? [id] : [restoredId, id], url });
      this.cursor = 0;
      return;
    }
    this.cursor = index;
    // The router rewrites the restored entry's state with this navigation's id.
    this.entries[index] = { ids: [...this.entries[index].ids, id], url };
  }
}

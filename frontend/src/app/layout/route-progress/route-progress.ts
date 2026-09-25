import { Component, DestroyRef, inject, signal } from '@angular/core';
import {
  NavigationCancel,
  NavigationEnd,
  NavigationError,
  NavigationStart,
  Router,
} from '@angular/router';

import { ROUTE_PROGRESS_DELAY_MS, ROUTE_PROGRESS_SETTLE_MS } from './route-progress.constants';
import { RouteProgressPhase } from './route-progress.model';

/**
 * Thin amber rail along the top of the content column while a route resolves.
 *
 * Most pages are eager and swap instantly, but the profile, the match detail and the rules are
 * lazy chunks: on a phone over mobile data, the tap used to be followed by nothing at all until
 * the chunk landed. The bar answers the tap without covering the page being left.
 */
@Component({
  selector: 'app-route-progress',
  templateUrl: './route-progress.html',
  host: { class: 'pointer-events-none absolute inset-x-0 top-0 z-40 block h-0.5' },
})
export class RouteProgress {
  protected readonly phase = signal<RouteProgressPhase>('idle');

  private showTimer: ReturnType<typeof setTimeout> | undefined;
  private hideTimer: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    const subscription = inject(Router).events.subscribe((event) => {
      if (event instanceof NavigationStart) {
        this.start();
      } else if (
        event instanceof NavigationEnd ||
        event instanceof NavigationCancel ||
        event instanceof NavigationError
      ) {
        this.finish();
      }
    });

    inject(DestroyRef).onDestroy(() => {
      subscription.unsubscribe();
      clearTimeout(this.showTimer);
      clearTimeout(this.hideTimer);
    });
  }

  private start(): void {
    clearTimeout(this.showTimer);
    clearTimeout(this.hideTimer);
    this.showTimer = setTimeout(() => this.phase.set('running'), ROUTE_PROGRESS_DELAY_MS);
  }

  private finish(): void {
    clearTimeout(this.showTimer);
    if (this.phase() !== 'running') {
      return;
    }
    this.phase.set('done');
    this.hideTimer = setTimeout(() => this.phase.set('idle'), ROUTE_PROGRESS_SETTLE_MS);
  }
}

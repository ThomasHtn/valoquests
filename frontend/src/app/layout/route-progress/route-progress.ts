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
 * Thin rail atop the content while a route resolves, for lazy chunks on slow networks.
 */
@Component({
  selector: 'app-route-progress',
  templateUrl: './route-progress.html',
  styleUrl: './route-progress.scss',
})
export class RouteProgress {
  /**
   * Current state of the rail, driving its animation.
   */
  protected readonly phase = signal<RouteProgressPhase>('idle');

  /**
   * Delay before the rail shows, so fast navigations never flash it.
   */
  private showTimer: ReturnType<typeof setTimeout> | undefined;

  /**
   * Delay before the completed rail resets to idle.
   */
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

  /**
   * Schedules the rail to show if the navigation outlasts the delay.
   */
  private start(): void {
    clearTimeout(this.showTimer);
    clearTimeout(this.hideTimer);
    this.showTimer = setTimeout(() => this.phase.set('running'), ROUTE_PROGRESS_DELAY_MS);
  }

  /**
   * Cancels a pending rail, or completes a running one and hides it after it settles.
   */
  private finish(): void {
    clearTimeout(this.showTimer);
    if (this.phase() !== 'running') {
      return;
    }
    this.phase.set('done');
    this.hideTimer = setTimeout(() => this.phase.set('idle'), ROUTE_PROGRESS_SETTLE_MS);
  }
}

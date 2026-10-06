import { DOCUMENT, effect, inject, Service, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { filter, fromEvent, interval } from 'rxjs';

import { SynchronizationApi } from '@core/synchronization/synchronization-api';

import { Connectivity } from '../connectivity';
import { PublicResources } from '../public-resources';
import { resourceValue } from '../resource-state.utils';
import { LIVE_REFRESH_POLL_MS } from './live-refresh.constants';
import { liveRefreshStamp } from './live-refresh.utils';

/**
 * Reloads shared resources when {@link liveRefreshStamp} changes, so open tabs stay current.
 */
@Service()
export class LiveRefresh {
  /**
   * Document whose visibility changes trigger a status check.
   */
  private readonly document = inject(DOCUMENT);

  /**
   * Synchronization API, whose status is polled for the refresh stamp.
   */
  private readonly synchronizationApi = inject(SynchronizationApi);

  /**
   * Shared resources reloaded on a refresh.
   */
  private readonly publicResources = inject(PublicResources);

  /**
   * Online state, to pause polling offline and catch the reconnection.
   */
  private readonly connectivity = inject(Connectivity);

  /**
   * Online state at the last check, to catch the moment it comes back.
   */
  private wasOnline = true;

  /**
   * Last read stamp, `null` until the first read settles.
   */
  private stamp: string | null = null;

  constructor() {
    // Polling offline would only turn every screen into an error state; a hidden tab catches up on return.
    interval(LIVE_REFRESH_POLL_MS)
      .pipe(
        filter(() => this.connectivity.online() && this.document.visibilityState === 'visible'),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.synchronizationApi.status.reload());

    // Back online: reload whatever failed meanwhile.
    effect(() => {
      const online = this.connectivity.online();
      if (online && !this.wasOnline) {
        this.publicResources.reload();
      }
      this.wasOnline = online;
    });

    fromEvent(this.document, 'visibilitychange')
      .pipe(
        filter(() => this.connectivity.online() && this.document.visibilityState === 'visible'),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.synchronizationApi.status.reload());

    effect(() => {
      // A loading value is stale or default, so wait for the read to settle.
      if (this.synchronizationApi.status.isLoading()) {
        return;
      }

      const status = resourceValue(this.synchronizationApi.status, null);
      if (!status) {
        return;
      }

      // A pass that imported nothing changed nothing, so only an import or a failed refresh reloads.
      const stamp = liveRefreshStamp(status.lastImportedAt, new Date());
      const isStale = untracked(this.publicResources.isStale);
      if (this.stamp !== null && (stamp !== this.stamp || isStale)) {
        this.publicResources.refresh();
      }
      this.stamp = stamp;
    });
  }
}

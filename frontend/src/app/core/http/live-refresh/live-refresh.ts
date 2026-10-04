import { DOCUMENT, effect, inject, Service } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter, fromEvent, interval } from 'rxjs';
import { CampaignApi } from '@core/campaign/campaign-api';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { PlayersApi } from '@core/players/players-api';
import { RankingApi } from '@core/ranking/ranking-api';
import { SynchronizationApi } from '@core/synchronization/synchronization-api';
import { Connectivity } from '../connectivity';
import { liveRefreshStamp } from './live-refresh.utils';
import { reloadAll, resourceValue } from '../resource-state.utils';
import { LIVE_REFRESH_POLL_MS } from './live-refresh.constants';

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
   * Players API, whose roster is reloaded on a refresh.
   */
  private readonly playersApi = inject(PlayersApi);

  /**
   * Campaign API, whose campaign, day and history are reloaded on a refresh.
   */
  private readonly campaignApi = inject(CampaignApi);

  /**
   * Ranking API, whose rankings are reloaded on a refresh.
   */
  private readonly rankingApi = inject(RankingApi);

  /**
   * Challenges API, whose current challenges are reloaded on a refresh.
   */
  private readonly challengesApi = inject(ChallengesApi);

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
    // Polling offline would only turn every screen into an error state.
    interval(LIVE_REFRESH_POLL_MS)
      .pipe(
        filter(() => this.connectivity.online()),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.synchronizationApi.status.reload());

    // Back online: reload whatever failed meanwhile.
    effect(() => {
      const online = this.connectivity.online();
      if (online && !this.wasOnline) {
        this.reloadEverything();
      }
      this.wasOnline = online;
    });

    fromEvent(this.document, 'visibilitychange')
      .pipe(
        filter(() => this.document.visibilityState === 'visible'),
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

      // The completion instant moves only once challenges and campaign are rebuilt.
      const stamp = liveRefreshStamp(status.lastCompletedAt, new Date());
      if (this.stamp !== null && stamp !== this.stamp) {
        this.reloadEverything();
      }
      this.stamp = stamp;
    });
  }

  /**
   * Reloads every shared resource, so all screens reflect the latest synchronization.
   */
  private reloadEverything(): void {
    reloadAll(
      this.synchronizationApi.status,
      this.playersApi.players,
      this.campaignApi.campaign,
      this.campaignApi.today,
      this.campaignApi.history,
      this.rankingApi.current,
      this.rankingApi.latestFinalizedWeek,
      this.rankingApi.history,
      this.rankingApi.daily,
      this.challengesApi.current,
    );
  }
}

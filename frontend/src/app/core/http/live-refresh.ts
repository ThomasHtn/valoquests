import { DOCUMENT, effect, inject, Service } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter, fromEvent, interval } from 'rxjs';
import { CampaignApi } from '@core/campaign/campaign-api';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { PlayersApi } from '@core/players/players-api';
import { RankingApi } from '@core/ranking/ranking-api';
import { Connectivity } from './connectivity';
import { liveRefreshStamp } from './live-refresh.utils';
import { reloadAll, resourceValue } from './resource-state.utils';
import { LIVE_REFRESH_POLL_MS, LIVE_REFRESH_SETTLE_MS } from './live-refresh.constants';

/**
 * Keeps every screen current without a page refresh.
 *
 * The backend rewrites its public data on its own schedule: a synchronization every thirty minutes,
 * the nightly tick at 00:10 and the Monday rollover. The shared `httpResource`s were fetched once
 * and never asked again, so a tab left open showed the squad's evening as it stood at load time.
 *
 * Polls the roster every minute (the sidebar's last-sync label already needed it), compares the
 * {@link liveRefreshStamp} to the previous one and, on a change, reloads the campaign, ranking and
 * challenge resources. A tab coming back to the foreground polls at once instead of waiting.
 *
 * Started by the shell, which lives as long as the app does.
 */
@Service()
export class LiveRefresh {
  private readonly document = inject(DOCUMENT);
  private readonly playersApi = inject(PlayersApi);
  private readonly campaignApi = inject(CampaignApi);
  private readonly rankingApi = inject(RankingApi);
  private readonly challengesApi = inject(ChallengesApi);
  private readonly connectivity = inject(Connectivity);

  /**
   * Whether the device was online at the last check, to catch the moment it comes back.
   */
  private wasOnline = true;

  /**
   * Stamp of the roster as last read; `null` until the first read has settled.
   */
  private stamp: string | null = null;

  /**
   * Pending reload, pushed back by every new change so a whole batch reloads once.
   */
  private pending: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    // Polling offline would only turn every screen into an error state until the network returns.
    interval(LIVE_REFRESH_POLL_MS)
      .pipe(
        filter(() => this.connectivity.online()),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.playersApi.players.reload());

    // Back online: whatever failed meanwhile is read again, without asking the reader to retry.
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
      .subscribe(() => this.playersApi.players.reload());

    effect(() => {
      // While loading, the value is either the default or the previous read: neither says anything
      // about the backend now, so the comparison waits for the read to settle.
      if (this.playersApi.players.isLoading()) {
        return;
      }

      const players = resourceValue(this.playersApi.players, null);
      if (players === null) {
        return;
      }

      const stamp = liveRefreshStamp(players, new Date());
      if (this.stamp !== null && stamp !== this.stamp) {
        this.scheduleReload();
      }
      this.stamp = stamp;
    });
  }

  private scheduleReload(): void {
    if (this.pending !== null) {
      clearTimeout(this.pending);
    }
    this.pending = setTimeout(() => {
      this.pending = null;
      this.reloadEverything();
    }, LIVE_REFRESH_SETTLE_MS);
  }

  private reloadEverything(): void {
    reloadAll(
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

import { DOCUMENT, effect, inject, Service } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter, fromEvent, interval } from 'rxjs';

import { CampaignApi } from '@core/campaign/campaign-api';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { PlayersApi } from '@core/players/players-api';
import { RankingApi } from '@core/ranking/ranking-api';

import { liveRefreshStamp } from './live-refresh.utils';
import { reloadAll, resourceValue } from './resource-state.utils';

/**
 * How often the roster is re-read to detect a change on the backend.
 */
export const LIVE_REFRESH_POLL_MS = 60_000;

/**
 * Delay between detecting a change and reloading the screens.
 *
 * A player's synchronization instant is written before the replay that follows it has committed, so
 * a reload fired the second the change is seen could still read the base as it stood before.
 */
export const LIVE_REFRESH_SETTLE_MS = 20_000;

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

  /**
   * Stamp of the roster as last read; `null` until the first read has settled.
   */
  private stamp: string | null = null;

  /**
   * Pending reload, so two changes seen in a row reload once.
   */
  private pending: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    interval(LIVE_REFRESH_POLL_MS)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.playersApi.players.reload());

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
      reloadAll(
        this.campaignApi.campaign,
        this.campaignApi.today,
        this.campaignApi.history,
        this.rankingApi.current,
        this.rankingApi.latestFinalizedWeek,
        this.rankingApi.history,
        this.rankingApi.daily,
        this.challengesApi.current,
      );
    }, LIVE_REFRESH_SETTLE_MS);
  }
}

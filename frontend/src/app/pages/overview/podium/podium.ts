import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { RankingApi } from '@core/ranking/ranking-api';
import { resolveChampionPlayerId } from '@core/ranking/ranking-champion.utils';
import { RankingEntry } from '@core/ranking/ranking.model';
import { resolvePositionBadgeClass } from '@core/ranking/ranking-visual.utils';
import { Avatar } from '@shared/avatar/avatar';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { PointsBadge } from '@shared/points-badge/points-badge';
import { PositionBadge } from '@shared/position-badge/position-badge';
import { ResourceState } from '@shared/resource-state/resource-state';

/**
 * Hero podium of the overview page: the top 3 players in hexagon-framed avatars, echoing the
 * hexagon position badge already used by the weekly ranking, followed by a compact strip of the
 * remaining tracked players.
 *
 * Reads the same shared current-ranking resource as `WeeklyRanking` directly, rather than reaching
 * into that component's internals, so both stay independent and `WeeklyRanking` is left unchanged.
 */
@Component({
  selector: 'app-podium',
  imports: [
    TranslatePipe,
    RouterLink,
    Avatar,
    ChampionBadge,
    PointsBadge,
    PositionBadge,
    ResourceState,
  ],
  templateUrl: './podium.html',
})
export class Podium {
  /**
   * Data-access service backing the shared current-ranking resource.
   */
  private readonly rankingApi = inject(RankingApi);

  /**
   * Reactive resource fetching the current week's ranking, shared with `WeeklyRanking` and the
   * overview header.
   */
  protected readonly rankingResource = this.rankingApi.current;

  /**
   * Id of the reigning weekly "Champion", or `null` while unknown or before any week has been
   * finalized.
   */
  protected readonly championPlayerId = computed(() =>
    resolveChampionPlayerId(resourceValue(this.rankingApi.latestFinalizedWeek, null)),
  );

  /**
   * Every active tracked player's current ranking entry, in the order returned by the backend.
   *
   * An inactive player is excluded: the podium and its preview strip are a competitive
   * leaderboard, and an inactive player never competes for a slot. The backend omits `position`
   * from the JSON entirely when null, so the parsed value is `undefined` rather than `null` -
   * `!= null` catches both.
   */
  protected readonly entries = computed<readonly RankingEntry[]>(
    () =>
      resourceValue(this.rankingResource, null)?.ranking.filter(
        (entry) => entry.position != null,
      ) ?? [],
  );

  /**
   * The top 3 ranking entries, shown as the podium spotlight.
   */
  protected readonly top3 = computed(() => this.entries().slice(0, 3));

  /**
   * Ranking entries from the 4th place onward, shown as a compact preview strip.
   */
  protected readonly rest = computed(() => this.entries().slice(3));

  /**
   * Resolves a player's avatar asset from their portrait field, exposed to the template.
   */
  protected readonly resolveAvatarUrl = resolvePlayerAvatarUrl;

  /**
   * Resolves the text color for a podium entry's position, exposed to the template.
   */
  protected readonly podiumTextAccent = resolvePositionBadgeClass;

  /**
   * Reloads the ranking resource after a failure.
   */
  protected reload(): void {
    this.rankingResource.reload();
  }
}

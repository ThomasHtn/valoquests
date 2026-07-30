import { Component, computed, inject, signal } from '@angular/core';

import { formatDateRange, isoWeekNumber } from '@core/date/week-period.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { PlayersApi } from '@core/players/players-api';
import { RankingApi } from '@core/ranking/ranking-api';
import { resolvePositionBadgeClass } from '@core/ranking/ranking-visual.utils';
import { Avatar } from '@shared/avatar/avatar';
import { Pagination } from '@shared/pagination/pagination';
import { PointsBadge } from '@shared/points-badge/points-badge';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SKELETON_ROWS } from '@shared/resource-state/skeleton.constants';
import { RankingHistoryWeekView } from './ranking.model';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';

/**
 * Ranking history page.
 *
 * Lists the finalized ranking of every completed calendar week, one week at a time, paginated
 * from the most recent to the oldest.
 */
@Component({
  selector: 'app-ranking',
  imports: [TranslatePipe, Avatar, Pagination, PointsBadge, ResourceState],
  templateUrl: './ranking.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Ranking {
  /**
   * Data-access service backing the ranking history resource.
   */
  private readonly rankingApi = inject(RankingApi);

  /**
   * Data-access service backing the shared players resource, used to resolve each row's avatar.
   */
  private readonly playersApi = inject(PlayersApi);

  /**
   * i18n service used to resolve the translated week label for each historical week.
   */
  private readonly translation = inject(Translation);

  /**
   * Reactive resource fetching every tracked player's summary, used to resolve avatars.
   */
  private readonly playersResource = this.playersApi.players;

  /**
   * Zero-based index of the requested page of ranking history.
   */
  protected readonly page = signal(0);

  /**
   * Reactive resource fetching the requested page of ranking history.
   */
  protected readonly historyResource = this.rankingApi.history(this.page);

  /**
   * Total number of available pages of ranking history.
   */
  protected readonly totalPages = computed(
    () => resourceValue(this.historyResource, null)?.totalPages ?? 0,
  );

  /**
   * Whether either backing resource is still loading.
   */
  protected readonly isLoading = anyLoading(this.historyResource, this.playersResource);

  /**
   * Whether either backing resource failed to load.
   */
  protected readonly hasError = anyError(this.historyResource, this.playersResource);

  /**
   * Avatar URL per player id, resolved from the shared players resource.
   */
  private readonly avatarUrlByPlayerId = computed(
    () =>
      new Map(
        resourceValue(this.playersResource, []).map(
          (player) => [player.id, resolvePlayerAvatarUrl(player.portrait)] as const,
        ),
      ),
  );

  /**
   * Finalized weeks of the requested page, mapped to display-ready blocks.
   */
  protected readonly weeks = computed<readonly RankingHistoryWeekView[]>(() => {
    const avatarUrlByPlayerId = this.avatarUrlByPlayerId();

    return (resourceValue(this.historyResource, null)?.content ?? []).map((week) => ({
      weekStart: week.weekStart,
      weekLabel: this.translation.translate('ranking.week.label', {
        number: isoWeekNumber(week.weekStart),
      }),
      dateRangeLabel: formatDateRange(week.weekStart, week.weekEnd),
      rows: week.ranking.map((entry) => ({
        position: entry.position,
        playerId: entry.playerId,
        displayName: entry.displayName,
        avatarUrl: avatarUrlByPlayerId.get(entry.playerId) ?? null,
        points: entry.points,
        completedChallenges: entry.completedChallenges,
      })),
    }));
  });

  /**
   * Resolves the badge classes for a row's position, exposed to the template.
   */
  protected readonly positionBadgeClass = resolvePositionBadgeClass;

  /**
   * Placeholder line widths driving the loading skeleton.
   */
  protected readonly skeletonRows = SKELETON_ROWS;

  /**
   * Reloads both backing resources after a failure.
   *
   * Both are retried because {@link hasError} reports their combined state and cannot tell which
   * one failed.
   */
  protected reload(): void {
    reloadAll(this.historyResource, this.playersResource);
  }
}

import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';

import { formatDateRange, isoWeekNumber } from '../../core/date/week-period';
import { TranslatePipe } from '../../core/i18n/translate-pipe';
import { Translation } from '../../core/i18n/translation';
import { resolvePlayerAvatarUrl } from '../../core/players/player-avatar';
import { PlayersApi } from '../../core/players/players-api';
import { RankingApi } from '../../core/ranking/ranking-api';
import { resolvePositionBadgeClass } from '../../core/ranking/ranking.constants';
import { Avatar } from '../../shared/avatar/avatar';
import { Pagination } from '../../shared/pagination/pagination';
import { ResourceState } from '../../shared/resource-state/resource-state';
import { RankingHistoryWeekView } from './ranking.model';

/**
 * Ranking history page.
 *
 * Lists the finalized ranking of every completed calendar week, one week at a time, paginated
 * from the most recent to the oldest.
 */
@Component({
  selector: 'app-ranking',
  imports: [TranslatePipe, Avatar, Pagination, ResourceState],
  templateUrl: './ranking.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Ranking {
  /**
   * Zero-based index of the requested page of ranking history.
   */
  protected readonly page = signal(0);
  /**
   * Total number of available pages of ranking history.
   */
  protected readonly totalPages = computed(() => this.historyResource.value()?.totalPages ?? 0);
  /**
   * Resolves the badge classes for a row's position, exposed to the template.
   */
  protected readonly positionBadgeClass = resolvePositionBadgeClass;
  /**
   * Data-access service backing the ranking history resource.
   */
  private readonly rankingApi = inject(RankingApi);
  /**
   * Reactive resource fetching the requested page of ranking history.
   */
  protected readonly historyResource = this.rankingApi.history(this.page);
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
   * Whether either backing resource is still loading.
   */
  protected readonly isLoading = computed(
    () => this.historyResource.isLoading() || this.playersResource.isLoading(),
  );
  /**
   * Whether either backing resource failed to load.
   */
  protected readonly hasError = computed(
    () => !!this.historyResource.error() || !!this.playersResource.error(),
  );
  /**
   * Avatar URL per player id, resolved from the shared players resource.
   */
  private readonly avatarUrlByPlayerId = computed(
    () =>
      new Map(
        this.playersResource
          .value()
          .map((player) => [player.id, resolvePlayerAvatarUrl(player.portrait)] as const),
      ),
  );
  /**
   * Finalized weeks of the requested page, mapped to display-ready blocks.
   */
  protected readonly weeks = computed<readonly RankingHistoryWeekView[]>(() => {
    const avatarUrlByPlayerId = this.avatarUrlByPlayerId();

    return (
      this.historyResource.value()?.content.map((week) => ({
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
      })) ?? []
    );
  });
}

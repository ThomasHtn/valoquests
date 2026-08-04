import { Component, computed, inject, signal } from '@angular/core';

import { BossApi } from '@core/boss/boss-api';
import { resolveBossCategoryColorClass } from '@core/boss/boss-visual.utils';
import { formatDateRange, isoWeekNumber } from '@core/date/week-period.utils';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { PlayersApi } from '@core/players/players-api';
import { RankingApi } from '@core/ranking/ranking-api';
import { resolveChampionPlayerId } from '@core/ranking/ranking-champion.utils';
import { Avatar } from '@shared/avatar/avatar';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { Pagination } from '@shared/pagination/pagination';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SKELETON_ROWS } from '@shared/resource-state/skeleton.constants';
import { BossHistoryRow } from './boss.model';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';

/**
 * Boss history page.
 *
 * Lists the finalized boss confrontation of every completed calendar week, one week at a time,
 * paginated from the most recent to the oldest.
 */
@Component({
  selector: 'app-boss',
  imports: [TranslatePipe, Avatar, ChampionBadge, Pagination, ResourceState],
  templateUrl: './boss.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Boss {
  /**
   * Data-access service backing the boss history resource.
   */
  private readonly bossApi = inject(BossApi);

  /**
   * Data-access service backing the shared players resource, used to resolve the finishing
   * blow's avatar.
   */
  private readonly playersApi = inject(PlayersApi);

  /**
   * Data-access service backing the reigning-champion lookup.
   */
  private readonly rankingApi = inject(RankingApi);

  /**
   * i18n service used to resolve the translated week and category labels for each historical
   * week.
   */
  private readonly translation = inject(Translation);

  /**
   * Reactive resource fetching every tracked player's summary, used to resolve avatars.
   */
  private readonly playersResource = this.playersApi.players;

  /**
   * Zero-based index of the requested page of boss history.
   */
  protected readonly page = signal(0);

  /**
   * Reactive resource fetching the requested page of boss history.
   */
  protected readonly historyResource = this.bossApi.history(this.page);

  /**
   * Total number of available pages of boss history.
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
   * Id of the reigning weekly "Champion", or `null` while unknown or before any week has been
   * finalized.
   */
  private readonly championPlayerId = computed(() =>
    resolveChampionPlayerId(resourceValue(this.rankingApi.latestFinalizedWeek, null)),
  );

  /**
   * Finalized weeks of the requested page, mapped to display-ready blocks.
   */
  protected readonly weeks = computed<readonly BossHistoryRow[]>(() => {
    const avatarUrlByPlayerId = this.avatarUrlByPlayerId();
    const championPlayerId = this.championPlayerId();

    return (resourceValue(this.historyResource, null)?.content ?? []).map((week) => ({
      weekStart: week.weekStart,
      weekLabel: this.translation.translate('boss.week.label', {
        number: isoWeekNumber(week.weekStart),
      }),
      dateRangeLabel: formatDateRange(week.weekStart, week.weekEnd),
      bossName: week.boss.name,
      bossDescription: week.boss.description,
      categoryColorClass: resolveBossCategoryColorClass(week.boss.category),
      categoryLabel: this.translation.translate(`boss.category.${week.boss.category}`),
      effectiveHp: week.effectiveHp,
      totalDamageDealt: week.totalDamageDealt,
      defeated: week.defeated,
      defeatedByPlayerId: week.defeatedByPlayerId,
      defeatedByPlayerDisplayName: week.defeatedByPlayerDisplayName,
      defeatedByAvatarUrl: week.defeatedByPlayerId
        ? (avatarUrlByPlayerId.get(week.defeatedByPlayerId) ?? null)
        : null,
      defeatedByIsChampion:
        week.defeatedByPlayerId !== null && week.defeatedByPlayerId === championPlayerId,
      winStreak: week.winStreak,
    }));
  });

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

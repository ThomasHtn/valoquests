import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LucideChevronDown,
  LucideChevronRight,
  LucideChevronUp,
  LucideFlame,
} from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import {
  resolveCompetitiveTierIconUrl,
  resolveCompetitiveTierVisual,
  resolveTierOrdinal,
} from '@core/players/competitive-tier.utils';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import {
  extractRiotTag,
  formatHeadshotPercentage,
  formatKda,
  formatWinRate,
} from '@core/players/player-format.utils';
import { resolveKdaVisual, resolveWinRateVisual } from '@core/players/player-stats.utils';
import { resourceValue } from '@core/http/resource-state.utils';
import { PlayerSummary } from '@core/players/player-summary.model';
import { PlayersApi } from '@core/players/players-api';
import { RankingApi } from '@core/ranking/ranking-api';
import { resolveChampionPlayerId } from '@core/ranking/ranking-champion.utils';
import { Avatar } from '@shared/avatar/avatar';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { PageHeader } from '@layout/page-header/page-header';
import { ProgressBar } from '@shared/progress-bar/progress-bar';
import { RankIconView } from '@shared/rank-icon-view/rank-icon-view';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SKELETON_ROWS } from '@shared/resource-state/skeleton.constants';
import { PLAYER_SORT_COLUMNS, PlayerRow, PlayerSortKey } from './players.model';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';

/**
 * Players list page — "Escouade".
 *
 * Displays every tracked player's identity, rank and statistics, split into two groups (root
 * `CLAUDE.md`, `PlayerStatus`): the roster currently in the campaign, and — separately, not faded
 * — those out of it. Sortable by any column; defaults to competitive rank so the strongest
 * players surface first, same as before this lot.
 */
@Component({
  selector: 'app-players',
  imports: [
    TranslatePipe,
    NgTemplateOutlet,
    RouterLink,
    LucideChevronDown,
    LucideChevronRight,
    LucideChevronUp,
    LucideFlame,
    Avatar,
    ChampionBadge,
    ProgressBar,
    RankIconView,
    ResourceState,
    PageHeader,
  ],
  templateUrl: './players.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Players {
  /**
   * Data-access service backing the shared players resource.
   */
  private readonly playersApi = inject(PlayersApi);

  /**
   * Data-access service backing the reigning-champion lookup.
   */
  private readonly rankingApi = inject(RankingApi);

  /**
   * i18n service used to resolve each row's translated rank label.
   */
  private readonly translation = inject(Translation);

  /**
   * Id of the reigning weekly "Champion", or `null` while unknown or before any week has been
   * finalized.
   */
  private readonly championPlayerId = computed(() =>
    resolveChampionPlayerId(resourceValue(this.rankingApi.latestFinalizedWeek, null)),
  );

  /**
   * Reactive resource fetching every tracked player's summary.
   */
  protected readonly playersResource = this.playersApi.players;

  /**
   * Today's board, by operator: whether they played yet, and the streak they are on. The day is
   * the backend's own, so the dot never lights an hour early.
   */
  private readonly today = computed(() => {
    const byId = new Map<number, { readonly played: boolean; readonly streak: number }>();
    for (const entry of resourceValue(this.rankingApi.daily, null)?.ranking ?? []) {
      byId.set(entry.playerId, { played: entry.matchCount > 0, streak: entry.streakDays });
    }
    return byId;
  });

  /**
   * Placeholder line widths driving the loading skeleton.
   */
  protected readonly skeletonRows = SKELETON_ROWS;

  /**
   * The table's sortable columns, exposed for the header row.
   */
  protected readonly sortColumns = PLAYER_SORT_COLUMNS;

  /**
   * Column the table is sorted on. Defaults to rank, the table's original (and only) order.
   */
  protected readonly sortKey = signal<PlayerSortKey>('rank');

  /**
   * `1` for ascending, `-1` for descending. Rank and win rate/KDA/HS%/matches all default to
   * descending (best first); name defaults to ascending (A→Z) — set the first time each key is
   * picked, in {@link setSort}.
   */
  protected readonly sortDirection = signal<1 | -1>(-1);

  /**
   * Every tracked player mapped to a display-ready row, unsorted — {@link inCampaignRows} and
   * {@link outOfCampaignRows} each sort their own slice, so a change of sort key never moves a row
   * between the two groups.
   */
  private readonly allRows = computed<readonly PlayerRow[]>(() =>
    resourceValue(this.playersResource, []).map((player) => this.toRow(player)),
  );

  /**
   * Rows of players currently in the campaign, sorted on {@link sortKey}.
   */
  protected readonly inCampaignRows = computed(() =>
    this.sortRows(this.allRows().filter((row) => row.inCampaign)),
  );

  /**
   * Rows of players out of the campaign — a group of their own rather than a fade on the same list
   * (design-review.md §A8): they still play and clear challenges individually.
   */
  protected readonly outOfCampaignRows = computed(() =>
    this.sortRows(this.allRows().filter((row) => !row.inCampaign)),
  );

  /**
   * Every row, both groups combined — only for the empty/loading states, which do not care which
   * group a row belongs to.
   */
  protected readonly rows = computed<readonly PlayerRow[]>(() => this.allRows());

  /**
   * The context bar's caption line: how the roster splits between the campaign and outside it.
   *
   * The eyebrow used to repeat the heading word for word — "Escouade" over "Escouade", under a
   * navigation entry already reading "Escouade" — which spent the one line above the title saying
   * nothing. The split is the fact this page exists to report and the one the campaign's every
   * denominator counts on, so it belongs there. Empty while the roster is still loading rather than
   * announcing a count of zero it is about to contradict.
   */
  protected readonly rosterEyebrow = computed(() => {
    const active = this.inCampaignRows().length;
    const out = this.outOfCampaignRows().length;

    if (active + out === 0) {
      return '';
    }

    const activeLabel = this.translation.translate('players.eyebrow.inCampaign', { count: active });

    return out === 0
      ? activeLabel
      : `${activeLabel} · ${this.translation.translate('players.eyebrow.outOfCampaign', { count: out })}`;
  });

  /**
   * Resolves the text and bar colors for a row's win rate, exposed to the template.
   */
  protected readonly winRateVisual = resolveWinRateVisual;

  /**
   * Resolves the text color for a row's KDA, exposed to the template.
   */
  protected readonly kdaVisual = resolveKdaVisual;

  /**
   * Formats a row's win rate, exposed to the template.
   */
  protected readonly formatWinRate = formatWinRate;

  /**
   * Formats a row's KDA, exposed to the template.
   */
  protected readonly formatKda = formatKda;

  /**
   * Formats a row's headshot rate, exposed to the template.
   */
  protected readonly formatHeadshotPercentage = formatHeadshotPercentage;

  /**
   * Maps a tracked player's summary to a display-ready row, resolving its avatar, rank icon,
   * and translated rank label.
   *
   * @param player - The tracked player's summary.
   * @returns The corresponding display-ready row.
   */
  private toRow(player: PlayerSummary): PlayerRow {
    return {
      id: player.id,
      displayName: player.displayName,
      isChampion: player.id === this.championPlayerId(),
      tag: extractRiotTag(player.riotId),
      avatarUrl: resolvePlayerAvatarUrl(player.portrait),
      competitiveTier: player.competitiveTier,
      tier: resolveCompetitiveTierVisual(player.competitiveTier, (key) =>
        this.translation.translate(key),
      ),
      rankIconUrl: resolveCompetitiveTierIconUrl(player.competitiveTier),
      rankRating: player.rankRating,
      winRate: player.winRate,
      kda: player.kda,
      headshotPercentage: player.headshotPercentage,
      matchesPlayed: player.matchesPlayed,
      inCampaign: player.status === 'ACTIVE',
      playedToday: this.today().get(player.id)?.played ?? false,
      streakDays: this.today().get(player.id)?.streak ?? 0,
    };
  }

  /**
   * Sorts a slice of rows on {@link sortKey}/{@link sortDirection}. `null` values (not yet
   * synchronized) always sort last regardless of direction — a missing statistic is not the same
   * as the worst one.
   *
   * @param rows - The rows to sort, in place order preserved for ties.
   * @returns The sorted rows, as a new array.
   */
  private sortRows(rows: readonly PlayerRow[]): readonly PlayerRow[] {
    const key = this.sortKey();
    const direction = this.sortDirection();

    return [...rows].sort((a, b) => {
      if (key === 'name') {
        return direction * a.displayName.localeCompare(b.displayName);
      }

      if (key === 'rank') {
        const tierComparison =
          resolveTierOrdinal(b.competitiveTier) - resolveTierOrdinal(a.competitiveTier);
        const comparison =
          tierComparison !== 0 ? tierComparison : (b.rankRating ?? -1) - (a.rankRating ?? -1);
        return direction === -1 ? comparison : -comparison;
      }

      const valueA = a[key];
      const valueB = b[key];
      if (valueA === null && valueB === null) {
        return 0;
      }
      if (valueA === null) {
        return 1;
      }
      if (valueB === null) {
        return -1;
      }

      return direction * (valueA - valueB);
    });
  }

  /**
   * Sorts on a column, toggling direction when it is already the active one and picking the
   * column's own natural default otherwise (best-first for every statistic, A→Z for the name).
   *
   * @param key - The column clicked.
   */
  protected setSort(key: PlayerSortKey): void {
    if (this.sortKey() === key) {
      this.sortDirection.update((direction) => (direction === 1 ? -1 : 1));
      return;
    }

    this.sortKey.set(key);
    this.sortDirection.set(key === 'name' ? 1 : -1);
  }
}

import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  LucideArrowDownAZ,
  LucideArrowDownNarrowWide,
  LucideArrowDownWideNarrow,
  LucideArrowDownZA,
  LucideChevronDown,
  LucideChevronRight,
  LucideChevronUp,
} from '@lucide/angular';

import { primaryTitle } from '@core/campaign/titles/campaign-title.utils';
import { resolveTitleVisual } from '@core/campaign/titles/campaign-title-visual.utils';
import { WeeklyTitle } from '@core/campaign/titles/campaign-title.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import {
  resolveCompetitiveTierIconUrl,
  resolveCompetitiveTierVisual,
  resolveTierOrdinal,
} from '@core/players/competitive-tier/player-competitive-tier.utils';
import { resolvePlayerAvatarUrl } from '@core/players/avatar/player-avatar.utils';
import {
  extractRiotTag,
  formatHeadshotPercentage,
  formatKda,
  formatWinRate,
} from '@core/players/player-format.utils';
import { resolveKdaVisual, resolveWinRateVisual } from '@core/players/stats/player-stats.utils';
import { anyLoading, resourceValue } from '@core/http/resource-state.utils';
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
import { Select } from '@shared/select/select';
import { Tooltip } from '@shared/tooltip/tooltip';
import { SelectOption } from '@shared/select/select.model';
import { SKELETON_ROWS } from '@shared/resource-state/resource-state-skeleton.constants';
import { PLAYER_SORT_COLUMNS } from './players.constants';
import { PlayerRow, PlayerSortKey } from './players.model';
import {
  defaultSortDirection,
  readPlayerSort,
  toPlayerSortOrder,
  writePlayerSort,
} from './players.utils';
import { PAGE_LAYOUT_CLASS } from '@layout/page-layout.constants';
import { TitleBadge } from '@shared/title-badge/title-badge';

/**
 * "Escouade" page: every tracked player, in-campaign roster and out-of-campaign group apart.
 */
@Component({
  selector: 'app-players',
  imports: [
    Tooltip,
    Select,
    TranslatePipe,
    NgTemplateOutlet,
    RouterLink,
    LucideArrowDownAZ,
    LucideArrowDownNarrowWide,
    LucideArrowDownWideNarrow,
    LucideArrowDownZA,
    LucideChevronDown,
    LucideChevronRight,
    LucideChevronUp,
    Avatar,
    TitleBadge,
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
   * Players data access.
   */
  private readonly playersApi = inject(PlayersApi);

  /**
   * Ranking data access, for the champion and titles.
   */
  private readonly rankingApi = inject(RankingApi);

  /**
   * Translates rank labels and sort options.
   */
  private readonly translation = inject(Translation);

  /**
   * Id of the reigning weekly champion, `null` before any finalized week.
   */
  private readonly championPlayerId = computed(() =>
    resolveChampionPlayerId(resourceValue(this.rankingApi.latestFinalizedWeek, null)),
  );

  /**
   * Current week's title held by each player id.
   */
  private readonly titlesByPlayer = computed(() => {
    const byPlayer = new Map<number, WeeklyTitle>();
    for (const entry of resourceValue(this.rankingApi.current, null)?.ranking ?? []) {
      const title = primaryTitle(entry.titles);
      if (title !== null) {
        byPlayer.set(entry.player.id, title);
      }
    }
    return byPlayer;
  });

  /**
   * Every tracked player's summary.
   */
  protected readonly playersResource = this.playersApi.players;

  /**
   * Whether the roster has nothing to show yet; the minute poll reloads it in the background.
   */
  protected readonly isLoading = anyLoading(this.playersResource);

  /**
   * Placeholder line widths of the loading skeleton.
   */
  protected readonly skeletonRows = SKELETON_ROWS;

  /**
   * Sortable columns of the header row.
   */
  protected readonly sortColumns = PLAYER_SORT_COLUMNS;

  /**
   * Sort picker options, shown on phones where the header row is hidden.
   */
  protected readonly sortOptions = computed<readonly SelectOption<PlayerSortKey>[]>(() =>
    PLAYER_SORT_COLUMNS.map((column) => ({
      value: column.key,
      label: this.translation.translate(column.labelKey),
    })),
  );

  /**
   * Router, to write the sort into the address.
   */
  private readonly router = inject(Router);

  /**
   * Active route, whose query parameters carry the sort.
   */
  private readonly route = inject(ActivatedRoute);

  /**
   * Sort read from the address on arrival, so a link or a step back keeps it.
   */
  private readonly requestedSort = readPlayerSort(this.route.snapshot.queryParamMap);

  /**
   * Column the table is sorted on.
   */
  protected readonly sortKey = signal<PlayerSortKey>(this.requestedSort.key);

  /**
   * `1` ascending, `-1` descending.
   */
  protected readonly sortDirection = signal<1 | -1>(this.requestedSort.direction);

  /**
   * Current order, as the phone's toggle words it.
   */
  protected readonly sortOrder = computed(() =>
    toPlayerSortOrder(this.sortKey(), this.sortDirection()),
  );

  /**
   * Unsorted rows; each group sorts its own slice so a sort never moves a row across groups.
   */
  private readonly allRows = computed<readonly PlayerRow[]>(() =>
    resourceValue(this.playersResource, []).map((player) => this.toRow(player)),
  );

  /**
   * Sorted rows of players in the campaign.
   */
  protected readonly inCampaignRows = computed(() =>
    this.sortRows(this.allRows().filter((row) => row.inCampaign)),
  );

  /**
   * Sorted rows of players out of the campaign, a group of their own rather than faded.
   */
  protected readonly outOfCampaignRows = computed(() =>
    this.sortRows(this.allRows().filter((row) => !row.inCampaign)),
  );

  /**
   * Both groups combined, for the empty and loading states.
   */
  protected readonly rows = computed<readonly PlayerRow[]>(() => this.allRows());

  /**
   * Win rate text and bar colors.
   */
  protected readonly winRateVisual = resolveWinRateVisual;

  /**
   * KDA text color.
   */
  protected readonly kdaVisual = resolveKdaVisual;

  constructor() {
    // The sort lives in the address; replacing the entry keeps sorting out of the history.
    effect(() => {
      const queryParams = writePlayerSort(this.sortKey(), this.sortDirection());
      untracked(() =>
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams,
          queryParamsHandling: 'merge',
          replaceUrl: true,
        }),
      );
    });
  }

  /**
   * Formats a win rate.
   */
  protected readonly formatWinRate = (winRate: number | null): string =>
    formatWinRate(winRate, this.translation.language());

  /**
   * Formats a KDA.
   */
  protected readonly formatKda = (kda: number | null): string =>
    formatKda(kda, this.translation.language());

  /**
   * Formats a headshot rate.
   */
  protected readonly formatHeadshotPercentage = (percentage: number | null): string =>
    formatHeadshotPercentage(percentage, this.translation.language());

  /**
   * Maps a player summary to a display-ready row.
   */
  private toRow(player: PlayerSummary): PlayerRow {
    const title = this.titlesByPlayer().get(player.id) ?? null;
    return {
      id: player.id,
      displayName: player.displayName,
      isChampion: player.id === this.championPlayerId(),
      tag: extractRiotTag(player.riotId),
      avatarUrl: resolvePlayerAvatarUrl(player.portrait),
      title: title === null ? null : { key: title, ...resolveTitleVisual(title) },
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
    };
  }

  /**
   * Sorts rows on the current sort; `null` stats always sort last, missing is not worst.
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
   * Sorts on the column picked on a phone, in its natural direction.
   */
  protected pickSort(key: PlayerSortKey): void {
    this.sortKey.set(key);
    this.sortDirection.set(defaultSortDirection(key));
  }

  /**
   * Reverses the order on the current column.
   */
  protected toggleSortDirection(): void {
    this.sortDirection.update((direction) => (direction === 1 ? -1 : 1));
  }

  /**
   * Sorts on a header: toggles the direction if active, else starts in its natural direction.
   */
  protected setSort(key: PlayerSortKey): void {
    if (this.sortKey() === key) {
      this.sortDirection.update((direction) => (direction === 1 ? -1 : 1));
      return;
    }

    this.sortKey.set(key);
    this.sortDirection.set(defaultSortDirection(key));
  }
}

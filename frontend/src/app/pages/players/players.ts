import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import {
  LucideChevronDown,
  LucideChevronRight,
  LucideChevronUp,
  LucideDynamicIcon,
} from '@lucide/angular';

import { buildTitlesByPlayer } from '@core/campaign/titles/campaign-title.utils';
import { anyLoading, resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resolvePlayerAvatarUrl } from '@core/players/avatar/player-avatar.utils';
import {
  resolveCompetitiveTierIconUrl,
  resolveCompetitiveTierVisual,
} from '@core/players/competitive-tier/player-competitive-tier.utils';
import {
  extractRiotTag,
  formatHeadshotPercentage,
  formatKda,
  formatWinRate,
} from '@core/players/player-format.utils';
import { PlayerSummary } from '@core/players/player-summary.model';
import { PlayersApi } from '@core/players/players-api';
import { resolveKdaVisual, resolveWinRateVisual } from '@core/players/stats/player-stats.utils';
import { RankingApi } from '@core/ranking/ranking-api';
import { resolveChampionPlayerId } from '@core/ranking/ranking-champion.utils';
import { PageHeader } from '@layout/page-header/page-header';
import { PAGE_LAYOUT_CLASS } from '@layout/page-layout.constants';
import { Avatar } from '@shared/avatar/avatar';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { ProgressBar } from '@shared/progress-bar/progress-bar';
import { RankIconView } from '@shared/rank-icon-view/rank-icon-view';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SKELETON_ROWS } from '@shared/resource-state/resource-state-skeleton.constants';
import { Select } from '@shared/select/select';
import { SelectOption } from '@shared/select/select.model';
import { TitleBadge } from '@shared/title-badge/title-badge';
import { Tooltip } from '@shared/tooltip/tooltip';

import { PLAYER_SORT_COLUMNS, PLAYER_SORT_ORDER_ICONS } from './players.constants';
import { PlayerRow, PlayerSortKey } from './players.model';
import {
  defaultSortDirection,
  readPlayerSort,
  sortPlayerRows,
  toPlayerSortOrder,
  writePlayerSort,
} from './players.utils';

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
    LucideChevronDown,
    LucideChevronRight,
    LucideChevronUp,
    LucideDynamicIcon,
    Avatar,
    TitleBadge,
    ChampionBadge,
    ProgressBar,
    RankIconView,
    ResourceState,
    PageHeader,
  ],
  templateUrl: './players.html',
  styleUrl: './players.scss',
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
  private readonly titlesByPlayer = computed(() =>
    buildTitlesByPlayer(resourceValue(this.rankingApi.current, null)?.ranking ?? []),
  );

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
  private readonly sortOrder = computed(() =>
    toPlayerSortOrder(this.sortKey(), this.sortDirection()),
  );

  /**
   * Translated current order, the phone toggle's text.
   */
  protected readonly sortOrderLabel = computed(() =>
    this.translation.translate(`players.sort.order.${this.sortOrder()}`),
  );

  /**
   * Arrow drawn beside the phone toggle's text.
   */
  protected readonly sortOrderIcon = computed(() => PLAYER_SORT_ORDER_ICONS[this.sortOrder()]);

  /**
   * Translation key of the active header's direction, read out to assistive technology.
   */
  protected readonly sortDirectionKey = computed(() =>
    this.sortDirection() === 1 ? 'players.sort.ascending' : 'players.sort.descending',
  );

  /**
   * Unsorted rows of both groups; each group sorts its own slice so a sort never moves a row across groups.
   */
  protected readonly allRows = computed<readonly PlayerRow[]>(() =>
    resourceValue(this.playersResource, []).map((player) => this.toRow(player)),
  );

  /**
   * Sorted rows of players in the campaign.
   */
  protected readonly inCampaignRows = computed(() =>
    sortPlayerRows(
      this.allRows().filter((row) => row.inCampaign),
      this.sortKey(),
      this.sortDirection(),
    ),
  );

  /**
   * Sorted rows of players out of the campaign, a group of their own rather than faded.
   */
  protected readonly outOfCampaignRows = computed(() =>
    sortPlayerRows(
      this.allRows().filter((row) => !row.inCampaign),
      this.sortKey(),
      this.sortDirection(),
    ),
  );

  /**
   * Writes the sort into the address, replacing the entry to keep sorting out of the history.
   */
  constructor() {
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
   * Maps a player summary to a display-ready row.
   */
  private toRow(player: PlayerSummary): PlayerRow {
    const language = this.translation.language();
    return {
      id: player.id,
      displayName: player.displayName,
      isChampion: player.id === this.championPlayerId(),
      tag: extractRiotTag(player.riotId),
      avatarUrl: resolvePlayerAvatarUrl(player.portrait),
      title: this.titlesByPlayer().get(player.id) ?? null,
      competitiveTier: player.competitiveTier,
      tier: resolveCompetitiveTierVisual(player.competitiveTier, (key) =>
        this.translation.translate(key),
      ),
      rankIconUrl: resolveCompetitiveTierIconUrl(player.competitiveTier),
      rankRating: player.rankRating,
      winRate: player.winRate,
      winRateLabel: formatWinRate(player.winRate, language),
      winRateVisual: resolveWinRateVisual(player.winRate),
      kda: player.kda,
      kdaLabel: formatKda(player.kda, language),
      kdaVisual: resolveKdaVisual(player.kda),
      headshotPercentage: player.headshotPercentage,
      headshotPercentageLabel: formatHeadshotPercentage(player.headshotPercentage, language),
      matchesPlayed: player.matchesPlayed,
      inCampaign: player.status === 'ACTIVE',
    };
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
      this.toggleSortDirection();
    } else {
      this.pickSort(key);
    }
  }
}

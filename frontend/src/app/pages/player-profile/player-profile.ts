import {
  afterRenderEffect,
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  input,
  linkedSignal,
  signal,
  untracked,
  viewChild,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { LucideChevronLeft } from '@lucide/angular';

import { primaryTitle } from '@core/campaign/titles/campaign-title.utils';
import { isNotFound, resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { MatchDay } from '@core/matches/day/match-day.model';
import { groupMatchesByDay } from '@core/matches/day/match-day.utils';
import { FILTERABLE_GAME_MODES } from '@core/matches/game-mode/match-game-mode.constants';
import { GameMode } from '@core/matches/game-mode/match-game-mode.model';
import { Match } from '@core/matches/match.model';
import { MatchesApi } from '@core/matches/matches-api';
import { parseRouteId } from '@core/navigation/navigation-route-id.utils';
import { resolvePlayerAvatarUrl } from '@core/players/avatar/player-avatar.utils';
import {
  resolveCompetitiveTierIconUrl,
  resolveCompetitiveTierVisual,
} from '@core/players/competitive-tier/player-competitive-tier.utils';
import { extractRiotTag } from '@core/players/player-format.utils';
import { PlayersApi } from '@core/players/players-api';
import { RankingApi } from '@core/ranking/ranking-api';
import { resolveChampionPlayerId } from '@core/ranking/ranking-champion.utils';
import { RULE_ANCHOR } from '@core/rules/rule-anchor.constants';
import { Season } from '@core/seasons/season.model';
import { SeasonsApi } from '@core/seasons/seasons-api';
import { PageHeader } from '@layout/page-header/page-header';
import { PAGE_LAYOUT_CLASS } from '@layout/page-layout.constants';
import { Avatar } from '@shared/avatar/avatar';
import { Button } from '@shared/button/button';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { MatchHistory } from '@shared/match-history/match-history';
import { ProgressBar } from '@shared/progress-bar/progress-bar';
import { RankIconView } from '@shared/rank-icon-view/rank-icon-view';
import { ResourceState } from '@shared/resource-state/resource-state';
import { Select } from '@shared/select/select';
import { SelectOption } from '@shared/select/select.model';
import { StatTile } from '@shared/stat-tile/stat-tile';
import { TitleBadge } from '@shared/title-badge/title-badge';
import { Tooltip } from '@shared/tooltip/tooltip';

import {
  GAME_MODE_BUTTON_COUNTS,
  MAX_PROGRESSION_SEASONS,
  PROFILE_VIEWS,
  STAT_SKELETON_TILE_MODIFIERS,
} from './player-profile.constants';
import { ProfileView } from './player-profile.model';
import {
  buildNotFoundPlate,
  buildStatStrip,
  readProfileQuery,
  resolveCurrentSeasonId,
  resolveRequestedSeasonId,
  writeProfileQuery,
} from './player-profile.utils';
import { Progression } from './progression/progression';
import { SeasonPicker } from './season-picker/season-picker';

/**
 * Player profile: identity, rank, filtered stats and a match history loaded on scroll.
 */
@Component({
  selector: 'app-player-profile',
  imports: [
    TranslatePipe,
    RouterLink,
    Avatar,
    ChampionBadge,
    MatchHistory,
    RankIconView,
    ResourceState,
    SeasonPicker,
    Progression,
    Select,
    TitleBadge,
    PageHeader,
    LucideChevronLeft,
    Button,
    ProgressBar,
    StatTile,
    Tooltip,
  ],
  templateUrl: './player-profile.html',
  styleUrl: './player-profile.scss',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class PlayerProfile {
  /**
   * Rulebook anchors, for the daily-yield band's link.
   */
  protected readonly ruleAnchor = RULE_ANCHOR;

  /**
   * Player id from the `:id` route parameter.
   */
  public readonly id = input.required<string>();

  /**
   * Player details data access.
   */
  private readonly playersApi = inject(PlayersApi);

  /**
   * Match history data access.
   */
  private readonly matchesApi = inject(MatchesApi);

  /**
   * Seasons data access, for the season filter.
   */
  private readonly seasonsApi = inject(SeasonsApi);

  /**
   * Ranking data access, for the champion and title.
   */
  private readonly rankingApi = inject(RankingApi);

  /**
   * Translates the rank label and mode options.
   */
  private readonly translation = inject(Translation);

  /**
   * Gives the not-found plate its address and writes the state back to it.
   */
  private readonly router = inject(Router);

  /**
   * Route whose query parameters hold the view, mode and season.
   */
  private readonly route = inject(ActivatedRoute);

  /**
   * State read from the address on arrival, so a reload or a step back reopens it.
   */
  private readonly requested = readProfileQuery(this.route.snapshot.queryParamMap);

  /**
   * Numeric `id`, `null` when the route names no valid player.
   */
  private readonly playerId = computed(() => parseRouteId(this.id()));

  /**
   * Zero-based index of the last requested history page, advanced on scroll.
   */
  private readonly page = signal(0);

  /**
   * View on screen; the filter bar swaps with it since the two views have different scopes.
   */
  protected readonly viewMode = signal<ProfileView>(this.requested.view);

  /**
   * Matches fetched so far for the current filters; the resource holds one page at a time.
   */
  protected readonly matches = signal<readonly Match[]>([]);

  /**
   * Game mode filter, `null` (the default) for every mode.
   */
  protected readonly gameModeFilter = signal<GameMode | null>(this.requested.mode);

  /**
   * Every known season.
   */
  private readonly seasonsResource = this.seasonsApi.seasons;

  /**
   * Known seasons, empty while loading or on error (`value()` throws on error).
   */
  protected readonly seasons = computed(() => resourceValue(this.seasonsResource, []));

  /**
   * Season filter, `null` for every season; defaults to the current one once seasons load.
   * A `linkedSignal` keeps a user choice; `previous.source.length` tells "not loaded yet" apart.
   */
  private readonly seasonId = linkedSignal<readonly Season[], number | null>({
    source: this.seasons,
    computation: (seasons, previous) =>
      previous && previous.source.length > 0
        ? previous.value
        : resolveRequestedSeasonId(seasons, this.requested.season),
  });

  /**
   * Player the season-scoped reads target, `null` until the seasons first load so they start on the right one.
   */
  private readonly scopedPlayerId = computed(() =>
    this.seasonsResource.status() === 'loading' ? null : this.playerId(),
  );

  /**
   * Whether the history waits on the seasons before its first page.
   */
  protected readonly historyPending = computed(() => this.seasonsResource.status() === 'loading');

  /**
   * Player details, unscoped so a filter change never brings back the skeleton.
   */
  protected readonly detailsResource = this.playersApi.details(this.playerId);

  /**
   * Player details, `null` while loading or on error (`value()` throws on error).
   */
  protected readonly details = computed(() => resourceValue(this.detailsResource, null));

  /**
   * Statistics of the filtered matches, idle while every mode is shown.
   */
  protected readonly scopedDetailsResource = this.playersApi.scopedDetails(
    this.scopedPlayerId,
    this.gameModeFilter,
    this.seasonId,
  );

  /**
   * Stat strip of the filtered matches, `null` for every mode, while loading or on error.
   */
  protected readonly statStrip = computed(() => {
    const statistics = resourceValue(this.scopedDetailsResource, undefined)?.statistics;
    return statistics ? buildStatStrip(statistics, this.translation.language()) : null;
  });

  /**
   * Today's daily yield, `null` on a day without matches or while loading.
   */
  protected readonly todayYield = computed(() => {
    const dailyYield = this.details()?.dailyYield;
    return dailyYield && dailyYield.matchesToday > 0 ? dailyYield : null;
  });

  /**
   * Whether the route names no known player: invalid id or backend 404.
   */
  protected readonly notFound = computed(
    () => this.playerId() === null || isNotFound(this.detailsResource),
  );

  /**
   * Plate shown instead of the profile when not found.
   */
  protected readonly notFoundPlate = computed(() =>
    buildNotFoundPlate(
      (key) => this.translation.translate(key),
      'playerProfile.notFound',
      this.router.url,
    ),
  );

  /**
   * Requested page of match history.
   */
  protected readonly matchesResource = this.matchesApi.history(
    this.scopedPlayerId,
    this.page,
    this.gameModeFilter,
    this.seasonId,
  );

  /**
   * Every filterable mode; one never played just shows the empty state.
   */
  private readonly gameModeFilterOptions = computed<readonly SelectOption<GameMode>[]>(() =>
    FILTERABLE_GAME_MODES.map((mode) => ({
      value: mode,
      label: this.translation.translate(`playerProfile.matches.gameMode.${mode}`),
    })),
  );

  /**
   * Filter bar, whose width decides how many game modes get a button.
   */
  private readonly filterRow = viewChild<ElementRef<HTMLElement>>('filterRow');

  /**
   * Filter bar width; measured on the row since the `lg` sidebar takes part of the viewport.
   */
  private readonly filterRowWidth = signal(0);

  /**
   * Game modes given their own button, as many as the bar fits.
   */
  protected readonly primaryGameModes = computed<readonly GameMode[]>(() => {
    const width = this.filterRowWidth();
    const count =
      GAME_MODE_BUTTON_COUNTS.find((threshold) => width >= threshold.minRowWidthPx)?.count ?? 0;
    return FILTERABLE_GAME_MODES.slice(0, count);
  });

  /**
   * Game mode options left to the overflow menu.
   */
  protected readonly overflowGameModeOptions = computed<readonly SelectOption<GameMode>[]>(() =>
    this.gameModeFilterOptions().filter(
      (option) => !this.primaryGameModes().includes(option.value),
    ),
  );

  /**
   * Whether the selected mode is in the overflow, to tint its trigger.
   */
  protected readonly isOverflowGameModeActive = computed(() =>
    this.overflowGameModeOptions().some((option) => option.value === this.gameModeFilter()),
  );

  /**
   * Seasons the progression charts, defaulting to the current one.
   * Kept apart from `seasonId` so switching views never changes what the reader was looking at.
   */
  protected readonly progressionSeasonIds = linkedSignal<readonly Season[], readonly number[]>({
    source: this.seasons,
    computation: (seasons, previous) => {
      if (previous && previous.source.length > 0) {
        return previous.value;
      }
      const currentSeasonId = resolveCurrentSeasonId(seasons);
      return currentSeasonId === null ? [] : [currentSeasonId];
    },
  });

  /**
   * Every season id, newest first; colours come from this order so a curve keeps its colour.
   */
  protected readonly seasonOrder = computed(() => this.seasons().map((season) => season.id));

  /**
   * Most seasons the progression charts at once.
   */
  protected readonly maxProgressionSeasons = MAX_PROGRESSION_SEASONS;

  /**
   * History season as the picker's selection, empty for every season.
   */
  protected readonly seasonSelection = computed<readonly number[]>(() => {
    const seasonId = this.seasonId();
    return seasonId === null ? [] : [seasonId];
  });

  /**
   * Fetched matches grouped by day; depends on the language for the day labels.
   */
  protected readonly matchDays = computed<readonly MatchDay[]>(() =>
    groupMatchesByDay(this.matches(), this.translation.language()),
  );

  /**
   * Whether a later history page exists; stops the sentinel from requesting past the end.
   */
  protected readonly hasMoreMatches = computed(() =>
    this.matchesResource.hasValue()
      ? this.page() + 1 < this.matchesResource.value().totalPages
      : false,
  );

  /**
   * Whether a page past the first is loading; the first one shows the full-page skeleton.
   */
  protected readonly isLoadingMoreMatches = computed(
    () => this.matchesResource.isLoading() && this.page() > 0,
  );

  /**
   * Whether the filters differ from every mode of the current season.
   */
  protected readonly hasActiveFilters = computed(
    () =>
      this.gameModeFilter() !== null || this.seasonId() !== resolveCurrentSeasonId(this.seasons()),
  );

  /**
   * Riot ID tag, `null` when absent or not loaded.
   */
  protected readonly tag = computed(() => {
    const details = this.details();
    return details ? extractRiotTag(details.riotId) : null;
  });

  /**
   * Avatar URL, `null` when unavailable.
   */
  protected readonly avatarUrl = computed(() =>
    resolvePlayerAvatarUrl(this.details()?.portrait ?? null),
  );

  /**
   * Whether the player won the last finalized week.
   */
  protected readonly isChampion = computed(() => {
    const championPlayerId = resolveChampionPlayerId(
      resourceValue(this.rankingApi.latestFinalizedWeek, null),
    );
    return this.details()?.id === championPlayerId;
  });

  /**
   * Weekly title held this week, `null` when none or not loaded.
   */
  protected readonly title = computed(() => {
    const playerId = this.details()?.id;
    if (playerId === undefined) {
      return null;
    }
    const current = resourceValue(this.rankingApi.current, null);
    const entry = current?.ranking.find((candidate) => candidate.player.id === playerId);
    return entry ? primaryTitle(entry.titles) : null;
  });

  /**
   * Rank label and colour, `null` while loading.
   */
  protected readonly tier = computed(() => {
    const details = this.details();
    if (!details) {
      return null;
    }

    return resolveCompetitiveTierVisual(details.competitiveTier, (key) =>
      this.translation.translate(key),
    );
  });

  /**
   * Rank icon path, `null` when unavailable.
   */
  protected readonly rankIconUrl = computed(() => {
    const details = this.details();
    return details ? resolveCompetitiveTierIconUrl(details.competitiveTier) : null;
  });

  /**
   * Skeleton tile span modifiers, mirroring the loaded stat tiles.
   */
  protected readonly statSkeletonTileModifiers = STAT_SKELETON_TILE_MODIFIERS;

  /**
   * Buttons of the display switch.
   */
  protected readonly profileViews = PROFILE_VIEWS;

  /**
   * Wires the filter bar's measure, the address sync and the history paging.
   */
  constructor() {
    this.trackFilterRowWidth();
    this.syncAddressWithFilters();
    this.foldMatchPages();
  }

  /**
   * Requests the next history page unless it is the last or one is in flight.
   */
  protected loadMoreMatches(): void {
    if (!this.hasMoreMatches() || this.matchesResource.isLoading()) {
      return;
    }
    this.page.update((page) => page + 1);
  }

  /**
   * Applies a game mode filter (`null` for every mode) and restarts the history.
   */
  protected onGameModeFilterChange(gameMode: GameMode | null): void {
    // Same value: the resource would not refetch, leaving the cleared history empty.
    if (gameMode === this.gameModeFilter()) {
      return;
    }
    this.gameModeFilter.set(gameMode);
    this.restartMatchHistory();
  }

  /**
   * Applies the history picker's selection, empty for every season.
   */
  protected onSeasonSelectionChange(selection: readonly number[]): void {
    this.onSeasonFilterChange(selection.length > 0 ? selection[0] : null);
  }

  /**
   * Applies a season filter (`null` for every season) and restarts the history.
   */
  private onSeasonFilterChange(seasonId: number | null): void {
    if (seasonId === this.seasonId()) {
      return;
    }
    this.seasonId.set(seasonId);
    this.restartMatchHistory();
  }

  /**
   * Switches between the match history and the progression view.
   */
  protected onViewModeChange(viewMode: ProfileView): void {
    this.viewMode.set(viewMode);
  }

  /**
   * Restores every mode of the current season and restarts the history.
   */
  protected resetFilters(): void {
    this.gameModeFilter.set(null);
    this.seasonId.set(resolveCurrentSeasonId(this.seasons()));
    this.restartMatchHistory();
  }

  /**
   * Clears the history so the previous filters' matches never show while the new query loads.
   */
  private restartMatchHistory(): void {
    this.matches.set([]);
    this.page.set(0);
  }

  /**
   * Measures the filter bar so the mode buttons that do not fit move to the overflow.
   */
  private trackFilterRowWidth(): void {
    // After render: the filter bar only exists once the details have rendered.
    afterRenderEffect((onCleanup) => {
      const element = this.filterRow()?.nativeElement;
      if (!element) {
        return;
      }
      const observer = new ResizeObserver(([entry]) =>
        this.filterRowWidth.set(entry.contentRect.width),
      );
      observer.observe(element);
      onCleanup(() => observer.disconnect());
    });
  }

  /**
   * Keeps the address in step with the state, replacing the entry to stay out of the history.
   */
  private syncAddressWithFilters(): void {
    effect(() => {
      // Without the seasons, the default season cannot be told from a chosen one.
      const seasons = this.seasons();
      if (seasons.length === 0) {
        return;
      }
      const seasonId = this.seasonId();
      const queryParams = writeProfileQuery(
        {
          view: this.viewMode(),
          mode: this.gameModeFilter(),
          season: seasonId === null ? 'ALL' : seasonId,
        },
        resolveCurrentSeasonId(seasons),
      );
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
   * Folds each settled page into `matches`: page zero replaces the list, later pages append.
   */
  private foldMatchPages(): void {
    effect(() => {
      if (this.matchesResource.isLoading() || !this.matchesResource.hasValue()) {
        return;
      }
      const content = this.matchesResource.value().content;
      // Untracked: react to the resource settling, not to `page` changing.
      if (untracked(this.page) === 0) {
        this.matches.set(content);
      } else {
        this.matches.update((matches) => [...matches, ...content]);
      }
    });
  }
}

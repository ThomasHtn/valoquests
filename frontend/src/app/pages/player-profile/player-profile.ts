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
import { RouterLink } from '@angular/router';
import { LucideChevronRight, LucideEllipsisVertical, LucideLoaderCircle } from '@lucide/angular';

import { formatLocalTime } from '@core/date/date-time.utils';
import { resourceValue } from '@core/http/resource-state.utils';
import { resolveAgentInitial, resolveMatchScore } from '@core/matches/match-format.utils';
import { resolveResultAccentClass, resolveResultTextClass } from '@core/matches/match-visual.utils';
import { FILTERABLE_GAME_MODES, GameMode } from '@core/matches/game-mode.model';
import { Match } from '@core/matches/match.model';
import { MatchesApi } from '@core/matches/matches-api';
import { Season } from '@core/matches/season.model';
import { SeasonsApi } from '@core/matches/seasons-api';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import {
  resolveCompetitiveTierIconUrl,
  resolveCompetitiveTierVisual,
} from '@core/players/competitive-tier.utils';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import {
  extractRiotTag,
  formatHeadshotPercentage,
  formatKda,
  formatScore,
  formatWinRate,
} from '@core/players/player-format.utils';
import { resolveKdaVisual, resolveWinRateVisual } from '@core/players/player-stats.utils';
import { PlayersApi } from '@core/players/players-api';
import { RankingApi } from '@core/ranking/ranking-api';
import { resolveChampionPlayerId } from '@core/ranking/ranking-champion.utils';
import { Avatar } from '@shared/avatar/avatar';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { ProgressBar } from '@shared/progress-bar/progress-bar';
import { RankIconView } from '@shared/rank-icon-view/rank-icon-view';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SKELETON_ROWS } from '@shared/resource-state/skeleton.constants';
import { Select } from '@shared/select/select';
import { SelectOption } from '@shared/select/select.model';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';
import { MatchDay } from './match-day.model';
import { groupMatchesByDay } from './match-day.utils';

/**
 * Game modes shown as their own button in the game-mode filter's button group, in
 * {@link FILTERABLE_GAME_MODES} order. The remaining modes stay reachable through that group's
 * overflow menu rather than crowding the group itself.
 */
const PRIMARY_GAME_MODES: readonly GameMode[] = ['COMPETITIVE', 'UNRATED', 'DEATHMATCH'];

/**
 * Period the statistics and the match history are scoped to.
 */
type ViewMode = 'weekly' | 'global';

/**
 * The two periods, in the order the toggle offers them: the active week first, since that is what
 * the profile is opened for, then everything on record.
 */
const VIEW_MODES: readonly ViewMode[] = ['weekly', 'global'];

/**
 * Player-profile page.
 *
 * Displays one tracked player's identity, current competitive rank, aggregated statistics and
 * filterable match history, loaded a page at a time as the user scrolls toward the end of the
 * list.
 */
@Component({
  selector: 'app-player-profile',
  imports: [
    TranslatePipe,
    RouterLink,
    Avatar,
    ChampionBadge,
    ProgressBar,
    RankIconView,
    ResourceState,
    Select,
    LucideChevronRight,
    LucideEllipsisVertical,
    LucideLoaderCircle,
  ],
  templateUrl: './player-profile.html',
  host: {
    class: PAGE_LAYOUT_CLASS,
    '(document:click)': 'onDocumentClick($event)',
  },
})
export class PlayerProfile {
  /**
   * Internal player identifier, bound from the `:id` route parameter.
   */
  public readonly id = input.required<string>();

  /**
   * Data-access service backing the player-details resource.
   */
  private readonly playersApi = inject(PlayersApi);

  /**
   * Data-access service backing the match-history resource.
   */
  private readonly matchesApi = inject(MatchesApi);

  /**
   * Data-access service backing the shared seasons resource, used by the season filter.
   */
  private readonly seasonsApi = inject(SeasonsApi);

  /**
   * Data-access service backing the reigning-champion lookup.
   */
  private readonly rankingApi = inject(RankingApi);

  /**
   * i18n service used to resolve the player's translated rank label.
   */
  private readonly translation = inject(Translation);

  /**
   * Numeric form of {@link id}, as required by the backing resources.
   */
  protected readonly playerId = computed(() => Number(this.id()));

  /**
   * Zero-based index of the last requested page of match history.
   *
   * Advanced by {@link loadMoreMatches} as the user scrolls, rather than by explicit pagination
   * controls; {@link matches} accumulates every page fetched so far.
   */
  protected readonly page = signal(0);

  /**
   * Every match fetched so far for the current filters, oldest fetch first.
   *
   * Populated by the constructor's effect as {@link matchesResource} settles: reset to the fetched
   * page's content on page zero (a fresh query, following a filter change), appended to on every
   * later page (a scroll-triggered load). Kept as a plain signal, rather than derived straight from
   * {@link matchesResource}, since that resource only ever holds one page at a time.
   */
  protected readonly matches = signal<readonly Match[]>([]);

  /**
   * Selected game-mode filter. Statistics are always scoped to one concrete mode - an "all modes"
   * aggregate would mix incomparable queues (e.g. deathmatch with competitive) - so this defaults
   * to competitive rather than being nullable.
   */
  protected readonly gameModeFilter = signal<GameMode>('COMPETITIVE');

  /**
   * Whether the stats and match history are scoped to the active week or to all time.
   *
   * Defaults to `'weekly'`: this profile is read first and foremost to check progress toward the
   * active week's challenges, with the all-time record as secondary context.
   */
  protected readonly viewMode = signal<ViewMode>('weekly');

  /**
   * The two periods offered by the toggle, in display order, so the template renders them from one
   * list instead of repeating the button markup per option.
   */
  protected readonly viewModes = VIEW_MODES;

  /**
   * Reactive resource fetching every known season, used by the season filter.
   */
  protected readonly seasonsResource = this.seasonsApi.seasons;

  /**
   * Known seasons for the season filter, or an empty list while loading or on error.
   *
   * Guarded by {@link HttpResourceRef.hasValue}: reading `value()` while the resource is in an
   * error state throws, so it must never be called unconditionally.
   */
  protected readonly seasons = computed(() =>
    this.seasonsResource.hasValue() ? this.seasonsResource.value() : [],
  );

  /**
   * Selected season filter, or `null` to include every season.
   *
   * Defaults to the current season - the one {@link Season.active} entry, falling back to
   * {@link seasons}'s first (most-recent) entry if none is flagged active - once it loads.
   * Implemented as a `linkedSignal` rather than a plain signal seeded from an effect: the default
   * is (re)computed from {@link seasons}, but an explicit user selection - including "all seasons"
   * (`null`) - is preserved across re-renders instead of being overwritten back to the computed
   * default.
   *
   * The `previous.source.length > 0` guard distinguishes "seasons have not loaded yet" (recompute
   * the default once they do) from "seasons already loaded, and this may be a deliberate user
   * choice" (keep it) - `previous` itself is already truthy on that first, pre-load computation,
   * so checking its mere presence would freeze the default at `null` forever.
   */
  protected readonly seasonId = linkedSignal<readonly Season[], number | null>({
    source: this.seasons,
    computation: (seasons, previous) =>
      previous && previous.source.length > 0
        ? previous.value
        : PlayerProfile.resolveCurrentSeasonId(seasons),
  });

  /**
   * Active week's Monday, as `YYYY-MM-DD`, or `null` while it has not loaded yet or in the
   * `'global'` view.
   *
   * Read from the shared current-ranking resource rather than computed client-side, so it is
   * always the exact week the backend considers active - the same source `Overview` and
   * `Leaderboard` already rely on.
   */
  protected readonly activeWeekStart = computed<string | null>(() =>
    this.viewMode() === 'weekly'
      ? (resourceValue(this.rankingApi.current, null)?.weekStart ?? null)
      : null,
  );

  /**
   * Reactive resource fetching the requested player's detailed profile, scoped to the selected
   * game mode, season and - in the `'weekly'` view - the active week.
   */
  protected readonly detailsResource = this.playersApi.details(
    this.playerId,
    this.gameModeFilter,
    this.seasonId,
    this.activeWeekStart,
  );

  /**
   * Requested player's detailed profile, or `null` while loading or on error.
   *
   * Guarded by {@link HttpResourceRef.hasValue}: reading `value()` while the resource is in an
   * error state throws, so it must never be called unconditionally.
   */
  protected readonly details = computed(() =>
    this.detailsResource.hasValue() ? this.detailsResource.value() : null,
  );

  /**
   * Reactive resource fetching the requested page of match history.
   */
  protected readonly matchesResource = this.matchesApi.history(
    this.playerId,
    this.page,
    this.gameModeFilter,
    this.seasonId,
    this.activeWeekStart,
  );

  /**
   * Options offered by the game-mode filter.
   *
   * Restricted to the modes synchronization imports rather than every mode the backend enum
   * declares: the others are never stored, so they would be permanently dead options. Within that
   * set, every mode is offered rather than only those the player has played, since narrowing the
   * list further would require an extra endpoint; a mode with no match simply yields the empty
   * state.
   */
  protected readonly gameModeFilterOptions = computed<readonly SelectOption<GameMode>[]>(() =>
    FILTERABLE_GAME_MODES.map((mode) => ({
      value: mode,
      label: this.translation.translate(`playerProfile.matches.gameMode.${mode}`),
    })),
  );

  /**
   * Game modes rendered as their own button in the game-mode filter's button group.
   */
  protected readonly primaryGameModes = PRIMARY_GAME_MODES;

  /**
   * {@link gameModeFilterOptions}, narrowed to the modes not offered their own button - i.e. those
   * reachable only through the game-mode filter's overflow menu.
   */
  protected readonly overflowGameModeOptions = computed<readonly SelectOption<GameMode>[]>(() =>
    this.gameModeFilterOptions().filter((option) => !PRIMARY_GAME_MODES.includes(option.value)),
  );

  /**
   * Translated label of the selected game mode when it is one of {@link overflowGameModeOptions},
   * or `null` when a primary mode is selected. Shown on the overflow trigger itself so the active
   * filter stays legible even when it isn't one of the buttons rendered beside it.
   */
  protected readonly overflowGameModeActiveLabel = computed(
    () =>
      this.overflowGameModeOptions().find((option) => option.value === this.gameModeFilter())
        ?.label ?? null,
  );

  /**
   * Whether the game-mode filter's overflow menu is open.
   */
  protected readonly isGameModeMenuOpen = signal(false);

  /**
   * Wrapper around the overflow trigger and its panel, used to tell a click on either apart from a
   * click elsewhere on the page - see {@link onDocumentClick}.
   */
  private readonly gameModeMenu = viewChild<ElementRef<HTMLElement>>('gameModeMenu');

  /**
   * Options offered by the season filter, including the "all seasons" entry.
   */
  protected readonly seasonFilterOptions = computed<readonly SelectOption<number | null>[]>(() => [
    { value: null, label: this.translation.translate('playerProfile.filters.allSeasons') },
    ...this.seasons().map((season) => ({
      value: season.id,
      label: this.translation.translate('playerProfile.filters.season', { name: season.name }),
    })),
  ]);

  /**
   * Every match fetched so far, grouped into the days they were played on.
   *
   * Drives both the table and the card list, so a day's record is computed once. Recomputed on a
   * language switch too, since each day's label is spelled out in the active language.
   */
  protected readonly matchDays = computed<readonly MatchDay[]>(() =>
    groupMatchesByDay(this.matches(), this.translation.language()),
  );

  /**
   * Whether a page of match history past the last fetched one still exists.
   *
   * Guards both {@link loadMoreMatches} and the sentinel element the intersection observer
   * watches: without it, scrolling to the end of a fully-loaded list would keep firing requests
   * for a page past the last one.
   */
  protected readonly hasMoreMatches = computed(() =>
    this.matchesResource.hasValue()
      ? this.page() + 1 < this.matchesResource.value().totalPages
      : false,
  );

  /**
   * Whether a page beyond the first is currently being fetched.
   *
   * Distinct from {@link matchesResource}'s own `isLoading`, which also covers the very first
   * page: that one is reported through {@link ResourceState}'s full-page skeleton, while this one
   * drives the small loading row appended under the already-visible matches.
   */
  protected readonly isLoadingMoreMatches = computed(
    () => this.matchesResource.isLoading() && this.page() > 0,
  );

  /**
   * Marker element rendered right after the last loaded match, present only while
   * {@link hasMoreMatches} holds. Observed by the constructor's intersection observer, which
   * requests the next page as soon as this element scrolls into view.
   */
  private readonly loadMoreTrigger = viewChild<ElementRef<HTMLElement>>('loadMoreTrigger');

  /**
   * Whether the filters differ from their default: the current season's competitive statistics.
   */
  protected readonly hasActiveFilters = computed(
    () =>
      this.gameModeFilter() !== 'COMPETITIVE' ||
      this.seasonId() !== PlayerProfile.resolveCurrentSeasonId(this.seasons()),
  );

  /**
   * Placeholder line widths driving the loading skeletons.
   */
  protected readonly skeletonRows = SKELETON_ROWS;

  /**
   * Tag segment of the player's Riot ID, or `null` when absent or not yet loaded.
   */
  protected readonly tag = computed(() => {
    const details = this.details();
    return details ? extractRiotTag(details.riotId) : null;
  });

  /**
   * Resolved avatar URL for the player's associated agent, or `null` when unavailable.
   */
  protected readonly avatarUrl = computed(() =>
    resolvePlayerAvatarUrl(this.details()?.portrait ?? null),
  );

  /**
   * Whether this player holds the reigning weekly "Champion" title, earned by finishing 1st in
   * the most recently finalized week.
   */
  protected readonly isChampion = computed(() => {
    const championPlayerId = resolveChampionPlayerId(
      resourceValue(this.rankingApi.latestFinalizedWeek, null),
    );
    return this.details()?.id === championPlayerId;
  });

  /**
   * Translated label and color class for the player's current competitive rank, or `null` while
   * loading.
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
   * SVG icon path for the player's current competitive rank, or `null` if unavailable.
   */
  protected readonly rankIconUrl = computed(() => {
    const details = this.details();
    return details ? resolveCompetitiveTierIconUrl(details.competitiveTier) : null;
  });

  /**
   * Resolves the colors for the win-rate stat tile, exposed to the template.
   */
  protected readonly winRateVisual = resolveWinRateVisual;

  /**
   * Resolves the colors for the K/D stat tile and match rows, exposed to the template.
   */
  protected readonly kdaVisual = resolveKdaVisual;

  /**
   * Resolves a match row's leading-edge accent, exposed to the template.
   */
  protected readonly resultAccentClass = resolveResultAccentClass;

  /**
   * Shared column grid for every row of the desktop match-history grid (header, day-summary and
   * match rows alike), so their columns land at the same horizontal position however each row is
   * otherwise styled. A CSS Grid rather than an HTML `<table>`: match rows need a real inset
   * margin to read as nested under the day-summary row above them, and `margin` has no effect on
   * `<tr>`.
   *
   * The 6 stat columns are `fr`-based, not fixed widths: a fixed width keeps them pinned to their
   * own narrow band regardless of how wide the row grows, bunching every stat together at the
   * row's trailing edge instead of spreading across it.
   */
  protected readonly rowGridClass =
    'grid grid-cols-[minmax(0,2fr)_repeat(6,minmax(0,1fr))] items-center';

  /**
   * Resolves the colour carrying a match's result on the player's own score, exposed to the
   * template.
   */
  protected readonly resultTextClass = resolveResultTextClass;

  /**
   * Formats a match's start time, exposed to the template.
   */
  protected readonly matchTime = formatLocalTime;

  /**
   * Resolves the monogram standing in for a match's agent portrait, exposed to the template.
   */
  protected readonly agentInitial = resolveAgentInitial;

  /**
   * Formats a win rate, exposed to the template.
   */
  protected readonly formatWinRate = formatWinRate;

  /**
   * Formats a KDA ratio, exposed to the template.
   */
  protected readonly formatKda = formatKda;

  /**
   * Formats a headshot rate, exposed to the template.
   */
  protected readonly formatHeadshotPercentage = formatHeadshotPercentage;

  /**
   * Formats an average score (ADR or ACS), exposed to the template.
   */
  protected readonly formatScore = formatScore;

  /**
   * Resolves a match's round score, exposed to the template.
   */
  protected readonly matchScore = resolveMatchScore;

  constructor() {
    // Folds each settled page of `matchesResource` into `matches`, replacing it on page zero (a
    // fresh query following a filter change) and appending on every later page (a scroll-triggered
    // load). Reads `page` with `untracked`: this effect must only react to the resource actually
    // settling, not to `page` changing the instant a load starts, when `matchesResource.value()`
    // still holds the *previous* page's content.
    effect(() => {
      if (this.matchesResource.isLoading() || !this.matchesResource.hasValue()) {
        return;
      }

      const content = this.matchesResource.value().content;
      if (untracked(this.page) === 0) {
        this.matches.set(content);
      } else {
        this.matches.update((matches) => [...matches, ...content]);
      }
    });

    // Requests the next page once the trigger element scrolls into view. Registered as an
    // after-render effect, rather than a plain one, since the element only exists in the DOM once
    // `hasMoreMatches` has rendered it.
    afterRenderEffect((onCleanup) => {
      const element = this.loadMoreTrigger()?.nativeElement;
      if (!element) {
        return;
      }

      const observer = new IntersectionObserver((entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          this.loadMoreMatches();
        }
      });
      observer.observe(element);
      onCleanup(() => observer.disconnect());
    });
  }

  /**
   * Requests the next page of match history, appending it to {@link matches} once it settles.
   *
   * Called by the intersection observer registered in the constructor. Guarded against firing
   * past the last page or while a page is already in flight - the observer can otherwise fire
   * again before the previous request settles, e.g. while the page is still short enough that the
   * trigger element stays on screen after a page loads.
   */
  protected loadMoreMatches(): void {
    if (!this.hasMoreMatches() || this.matchesResource.isLoading()) {
      return;
    }
    this.page.update((page) => page + 1);
  }

  /**
   * Switches between the weekly and all-time views and restarts the match history from its first
   * page, since {@link activeWeekStart} changing scopes both the statistics and the match history
   * differently.
   *
   * @param mode - The newly selected view.
   */
  protected setViewMode(mode: ViewMode): void {
    if (this.viewMode() === mode) {
      return;
    }
    this.viewMode.set(mode);
    this.restartMatchHistory();
  }

  /**
   * Applies the selected game-mode filter and restarts the match history from its first page.
   * Called directly by the button group's own buttons and by {@link selectOverflowGameMode}.
   *
   * @param gameMode - The newly selected game mode.
   */
  protected onGameModeFilterChange(gameMode: GameMode): void {
    this.gameModeFilter.set(gameMode);
    this.restartMatchHistory();
  }

  /**
   * Toggles the game-mode filter's overflow menu.
   */
  protected toggleGameModeMenu(): void {
    this.isGameModeMenuOpen.update((isOpen) => !isOpen);
  }

  /**
   * Applies a game mode picked from the overflow menu and closes it.
   *
   * @param gameMode - The newly selected game mode.
   */
  protected selectOverflowGameMode(gameMode: GameMode): void {
    this.isGameModeMenuOpen.set(false);
    this.onGameModeFilterChange(gameMode);
  }

  /**
   * Closes the game-mode filter's overflow menu when a click lands outside its trigger and panel.
   *
   * @param event - The document-wide click event.
   */
  protected onDocumentClick(event: MouseEvent): void {
    if (
      this.isGameModeMenuOpen() &&
      !this.gameModeMenu()?.nativeElement.contains(event.target as Node)
    ) {
      this.isGameModeMenuOpen.set(false);
    }
  }

  /**
   * Applies the selected season filter and restarts the match history from its first page.
   *
   * @param seasonId - The newly selected season id, or `null` for every season.
   */
  protected onSeasonFilterChange(seasonId: number | null): void {
    this.seasonId.set(seasonId);
    this.restartMatchHistory();
  }

  /**
   * Restores the default filters - the current season's competitive statistics - and restarts the
   * match history from its first page.
   */
  protected resetFilters(): void {
    this.gameModeFilter.set('COMPETITIVE');
    this.seasonId.set(PlayerProfile.resolveCurrentSeasonId(this.seasons()));
    this.restartMatchHistory();
  }

  /**
   * Clears the accumulated match history and returns to its first page.
   *
   * Called on every filter change so the constructor's effect does not briefly show the previous
   * filters' matches while the new query is still loading.
   */
  private restartMatchHistory(): void {
    this.matches.set([]);
    this.page.set(0);
  }

  /**
   * Resolves the id of the current season - the one flagged {@link Season.active} - or the first
   * (most-recent) known season if none is active, or `null` if none are known yet.
   *
   * @param seasons - Known seasons, as returned by the seasons resource.
   */
  private static resolveCurrentSeasonId(seasons: readonly Season[]): number | null {
    return (seasons.find((season) => season.active) ?? seasons[0])?.id ?? null;
  }
}

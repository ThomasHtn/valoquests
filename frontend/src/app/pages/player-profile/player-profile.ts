import {
  Component,
  computed,
  effect,
  inject,
  input,
  linkedSignal,
  signal,
  untracked,
} from '@angular/core';
import { RouterLink } from '@angular/router';

import { primaryTitle } from '@core/campaign/campaign-title.utils';
import { resolveTitleVisual } from '@core/campaign/campaign-visual.utils';
import { resourceValue } from '@core/http/resource-state.utils';
import { FILTERABLE_GAME_MODES, GameMode } from '@core/matches/game-mode.model';
import { Match } from '@core/matches/match.model';
import { MatchesApi } from '@core/matches/matches-api';
import { formatSeasonName } from '@core/matches/season-name.utils';
import { Season } from '@core/matches/season.model';
import { SeasonsApi } from '@core/matches/seasons-api';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { RULE_ANCHOR } from '@core/rules/rule-anchor.constants';
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
import { PageHeader } from '@layout/page-header/page-header';
import { ProgressBar } from '@shared/progress-bar/progress-bar';
import { RankIconView } from '@shared/rank-icon-view/rank-icon-view';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SKELETON_ROWS } from '@shared/resource-state/skeleton.constants';
import { MultiSelect } from '@shared/multi-select/multi-select';
import { Select } from '@shared/select/select';
import { SelectOption } from '@shared/select/select.model';
import { StatTile } from '@shared/stat-tile/stat-tile';
import { Tooltip } from '@shared/tooltip/tooltip';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';
import { MatchDay } from './match-day.model';
import { groupMatchesByDay } from './match-day.utils';
import { MatchHistory } from './match-history/match-history';
import {
  DEFAULT_GAME_MODE,
  MAX_PROGRESSION_SEASONS,
  PRIMARY_GAME_MODES,
} from './player-profile.constants';
import { resolveCurrentSeasonId, resolveYieldToneClass } from './player-profile.utils';
import { Progression } from './progression/progression';
import { TitleBadge } from '@shared/title-badge/title-badge';

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
    MatchHistory,
    ProgressBar,
    RankIconView,
    ResourceState,
    MultiSelect,
    Progression,
    Select,
    TitleBadge,
    PageHeader,
    StatTile,
    Tooltip,
  ],
  templateUrl: './player-profile.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class PlayerProfile {
  /**
   * Named rulebook fragments, for the link out of the daily-yield band.
   */
  protected readonly ruleAnchor = RULE_ANCHOR;

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
   * Which of the profile's two views is on screen.
   *
   * The two do not share a scope: the match history is one mode and one season at a time, while
   * the progression view compares several seasons of competitive play. So the filter bar swaps
   * with the view rather than trying to drive both, and the summary tiles - which report the
   * history's filters - are shown with the history alone.
   */
  protected readonly viewMode = signal<'MATCHES' | 'PROGRESS'>('MATCHES');

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
  protected readonly gameModeFilter = signal<GameMode>(DEFAULT_GAME_MODE);

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
      previous && previous.source.length > 0 ? previous.value : resolveCurrentSeasonId(seasons),
  });

  /**
   * Reactive resource fetching the requested player's detailed profile, scoped to the selected
   * game mode and season.
   */
  protected readonly detailsResource = this.playersApi.details(
    this.playerId,
    this.gameModeFilter,
    this.seasonId,
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
   * Whether the selected game mode is one of {@link overflowGameModeOptions}, i.e. one the
   * segmented control does not render a button for.
   *
   * Drives the brand tint on that dropdown's trigger: without it, picking a mode from the list
   * would leave the whole control unmarked, since the three segments beside it are all inactive.
   */
  protected readonly isOverflowGameModeActive = computed(() =>
    this.overflowGameModeOptions().some((option) => option.value === this.gameModeFilter()),
  );

  /**
   * Seasons the progression view charts, defaulting to the current one.
   *
   * Kept apart from {@link seasonId}: the two views ask different questions of the same list, and
   * folding a multi-season selection back into the history's single-season filter would silently
   * change what the reader was looking at when they switch tabs. Same `linkedSignal` shape as
   * {@link seasonId}, so it seeds itself once the seasons load without overwriting a real choice.
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
   * Every known season's identifier, in the order the API returned them (newest first).
   *
   * Handed to the progression view as the source of each season's colour: taken from this list
   * rather than from the selection, a curve keeps its colour when another season is unticked.
   */
  protected readonly seasonOrder = computed(() => this.seasons().map((season) => season.id));

  /**
   * Options offered by the progression view's multi-season filter.
   *
   * No "all seasons" entry, unlike {@link seasonFilterOptions}: here the seasons are what the
   * chart's curves *are*, so "all of them" is a selection the reader makes, not a value.
   */
  protected readonly progressionSeasonOptions = computed<readonly SelectOption<number>[]>(() =>
    this.seasons().map((season) => ({ value: season.id, label: this.seasonName(season) })),
  );

  /**
   * Summary of the progression view's season selection, shown on its trigger.
   */
  protected readonly progressionSeasonLabel = computed(() => {
    const selected = this.progressionSeasonIds();
    if (selected.length === 0) {
      return this.translation.translate('playerProfile.filters.allSeasons');
    }
    if (selected.length === 1) {
      const season = this.seasons().find((entry) => entry.id === selected[0]);
      return season
        ? this.seasonName(season)
        : this.translation.translate('playerProfile.filters.seasonLabel');
    }
    return this.translation.translate('playerProfile.filters.seasonCount', {
      count: selected.length,
    });
  });

  /**
   * Largest number of seasons the progression view charts at once, exposed to the template.
   */
  protected readonly maxProgressionSeasons = MAX_PROGRESSION_SEASONS;

  /**
   * Options offered by the season filter, including the "all seasons" entry.
   */
  protected readonly seasonFilterOptions = computed<readonly SelectOption<number | null>[]>(() => [
    { value: null, label: this.translation.translate('playerProfile.filters.allSeasons') },
    // Bare season name rather than "Saison {name}": the filter is captioned "Saison" right beside
    // the trigger, so spelling it again on every option only widens the row.
    ...this.seasons().map((season) => ({ value: season.id, label: this.seasonName(season) })),
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
   * Whether the filters differ from their default: the current season's competitive statistics.
   */
  protected readonly hasActiveFilters = computed(
    () =>
      this.gameModeFilter() !== DEFAULT_GAME_MODE ||
      this.seasonId() !== resolveCurrentSeasonId(this.seasons()),
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
   * The one weekly title this player is decorated with this week, or `null` when they hold none
   * or their details have not loaded yet.
   */
  protected readonly title = computed(() => {
    const playerId = this.details()?.id;
    if (playerId === undefined) {
      return null;
    }
    const current = resourceValue(this.rankingApi.current, null);
    const entry = current?.ranking.find((candidate) => candidate.player.id === playerId);
    const key = entry ? primaryTitle(entry.titles) : null;
    return key === null ? null : { key, ...resolveTitleVisual(key) };
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
   * Formats a win rate, exposed to the template.
   */
  protected readonly formatWinRate = formatWinRate;

  /**
   * Formatters and colours of the stat strip, exposed to the template.
   */
  protected readonly formatKda = formatKda;

  protected readonly formatHeadshotPercentage = formatHeadshotPercentage;

  protected readonly formatScore = formatScore;

  protected readonly kdaVisual = resolveKdaVisual;

  /**
   * Colour of the share the next match keeps, exposed to the template.
   */
  protected readonly yieldToneClass = resolveYieldToneClass;

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
  }

  /**
   * Requests the next page of match history, appending it to {@link matches} once it settles.
   *
   * Called when the history's trailing sentinel scrolls into view. Guarded against firing
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
   * Applies the selected season filter and restarts the match history from its first page.
   *
   * @param seasonId - The newly selected season id, or `null` for every season.
   */
  protected onSeasonFilterChange(seasonId: number | null): void {
    this.seasonId.set(seasonId);
    this.restartMatchHistory();
  }

  /**
   * Switches between the match history and the progression view.
   *
   * @param viewMode - The newly selected view.
   */
  protected onViewModeChange(viewMode: 'MATCHES' | 'PROGRESS'): void {
    this.viewMode.set(viewMode);
  }

  /**
   * Restores the default filters - the current season's competitive statistics - and restarts the
   * match history from its first page.
   */
  protected resetFilters(): void {
    this.gameModeFilter.set(DEFAULT_GAME_MODE);
    this.seasonId.set(resolveCurrentSeasonId(this.seasons()));
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
   * Spells a season's raw code out in the active language.
   *
   * @param season - The season to label.
   * @returns The label to show in the filters.
   */
  private seasonName(season: Season): string {
    return formatSeasonName(season.name, (key, params) => this.translation.translate(key, params));
  }
}

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
import {
  LucideActivity,
  LucideChevronRight,
  LucideCrosshair,
  LucideGamepad2,
  LucideLoaderCircle,
  LucideSwords,
  LucideTarget,
  LucideZap,
} from '@lucide/angular';

import { formatLocalTime } from '@core/date/date-time.utils';
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
import { Avatar } from '@shared/avatar/avatar';
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
    ProgressBar,
    RankIconView,
    ResourceState,
    Select,
    LucideActivity,
    LucideChevronRight,
    LucideCrosshair,
    LucideGamepad2,
    LucideLoaderCircle,
    LucideSwords,
    LucideTarget,
    LucideZap,
  ],
  templateUrl: './player-profile.html',
  host: { class: PAGE_LAYOUT_CLASS },
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
   * Options offered by the season filter, including the "all seasons" entry.
   */
  protected readonly seasonFilterOptions = computed<readonly SelectOption<number | null>[]>(() => [
    { value: null, label: this.translation.translate('playerProfile.filters.allSeasons') },
    ...this.seasons().map((season) => ({ value: season.id, label: season.name })),
  ]);

  /**
   * Every match fetched so far, grouped into the days they were played on.
   *
   * Drives both the table and the card list, so a day's record is computed once.
   */
  protected readonly matchDays = computed<readonly MatchDay[]>(() =>
    groupMatchesByDay(this.matches()),
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
   * Applies the selected game-mode filter and restarts the match history from its first page.
   *
   * @param gameMode - The newly selected game mode. `null` never actually occurs here - the
   * game-mode select never offers a "no selection" option - but `Select#value` is typed `T | null`
   * for every consumer, so the emitted event carries that type regardless.
   */
  protected onGameModeFilterChange(gameMode: GameMode | null): void {
    if (gameMode === null) {
      return;
    }
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

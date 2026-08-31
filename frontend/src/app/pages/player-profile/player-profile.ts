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
import { LucideLoaderCircle } from '@lucide/angular';

import { NgTemplateOutlet } from '@angular/common';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { formatLocalTime } from '@core/date/date-time.utils';
import { resourceValue } from '@core/http/resource-state.utils';
import {
  resolveAgentImageUrl,
  resolveAgentInitial,
  resolveMapImageUrl,
  resolveMatchScore,
} from '@core/matches/match-format.utils';
import { resolveResultAccentClass, resolveResultTextClass } from '@core/matches/match-visual.utils';
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
import { RankingEntry } from '@core/ranking/ranking.model';
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
import { Breakpoint } from '@core/viewport/breakpoint';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';
import { MatchDay } from './match-day.model';
import { groupMatchesByDay } from './match-day.utils';
import { MediaThumbnail } from './media-thumbnail/media-thumbnail';
import { Progression } from './progression/progression';

/**
 * Game modes shown as their own button in the game-mode filter's button group, in
 * {@link FILTERABLE_GAME_MODES} order. The remaining modes stay reachable through that group's
 * overflow menu rather than crowding the group itself.
 */
const PRIMARY_GAME_MODES: readonly GameMode[] = ['COMPETITIVE', 'UNRATED', 'DEATHMATCH'];

/**
 * Largest number of seasons the progression view will chart at once.
 *
 * The chart series palette holds exactly this many slots, validated as an ordered set against the
 * page's surface; a sixth curve would have to be a generated hue, which is how a chart ends up
 * with two colours a colourblind reader cannot separate. See `styles/colors.css`.
 */
const MAX_PROGRESSION_SEASONS = 5;

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
    MediaThumbnail,
    NgTemplateOutlet,
    ProgressBar,
    RankIconView,
    ResourceState,
    MultiSelect,
    Progression,
    Select,
    LucideLoaderCircle,
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
   * Router, used to mirror the open half of the profile into the URL.
   */
  private readonly router = inject(Router);

  /**
   * Route, read once for the half a link arrived pointing at.
   */
  private readonly route = inject(ActivatedRoute);

  /**
   * i18n service used to resolve the player's translated rank label.
   */
  private readonly translation = inject(Translation);

  /**
   * Whether the viewport can hold the match grid: below it, the same matches are rendered as
   * cards. Only the matching layout is put in the DOM, never both.
   */
  protected readonly isLarge = inject(Breakpoint).isLarge;

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
   * Which half of the profile is open: this player's Valorant record, or what they brought to the
   * colony.
   *
   * Opens on the Valorant record — it is what a reader comes to a player's page for. Mirrored into
   * the URL by {@link onProfileTabChange} so a link can point at either half.
   */
  /**
   * The two halves, in the order they are shown.
   */
  protected readonly profileTabs = ['VALORANT', 'COLONY'] as const;

  protected readonly profileTab = signal<'COLONY' | 'VALORANT'>(
    this.route.snapshot.queryParamMap.get('tab') === 'colony' ? 'COLONY' : 'VALORANT',
  );

  /**
   * This player's line in the week's ranking, or `null` while it loads or if they have none.
   *
   * The whole colony tab reads from here: the ranking already carries the damage split, the days
   * played and the per-challenge progress, per player and per week. Nothing about the contribution
   * needed a new endpoint.
   */
  protected readonly contribution = computed<RankingEntry | null>(() => {
    const ranking = resourceValue(this.rankingApi.current, null);
    return ranking?.ranking.find((entry) => entry.player.id === this.playerId()) ?? null;
  });

  /**
   * What this player's week actually took off the boss: their total, less the regularity bonus.
   *
   * The bonus rewards turning up rather than producing, so it never touches the boss — the rule the
   * rulebook states and `BossCampaign` already applies when it sums its contributions. Stated here
   * because a player has no other way of seeing it.
   */
  protected readonly bossDamage = computed<number>(() => {
    const entry = this.contribution();
    return entry === null ? 0 : Math.max(0, entry.totalDamage - entry.regularityBonus);
  });

  /**
   * The four sources of a week's damage, as rows for the split, widest share first in the bar.
   */
  protected readonly damageSplit = computed(() => {
    const entry = this.contribution();
    if (entry === null) {
      return [];
    }

    const total = Math.max(1, entry.totalDamage);

    return [
      { key: 'matches', value: entry.matchDamage, colorClass: 'bg-brand-500' },
      { key: 'challenges', value: entry.challengeDamage, colorClass: 'bg-accent-cyan' },
      { key: 'regularity', value: entry.regularityBonus, colorClass: 'bg-accent-violet' },
      { key: 'squad', value: entry.teamBonus, colorClass: 'bg-accent-blue' },
    ].map((row) => ({ ...row, percentage: Math.round((row.value / total) * 100) }));
  });

  /**
   * The week's seven days, each flagged with whether this player played it.
   *
   * The ranking reports how many days were played, not which ones, so the strip fills from the left
   * rather than landing each mark on its own weekday: it is a count drawn as a row, and labelling
   * the cells Monday to Sunday would claim a precision the figure does not carry.
   */
  protected readonly activeDayStrip = computed<readonly boolean[]>(() => {
    const played = this.contribution()?.activeDays ?? 0;
    return Array.from({ length: 7 }, (_unused, index) => index < played);
  });

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
      const currentSeasonId = PlayerProfile.resolveCurrentSeasonId(seasons);
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
   * The 8 stat columns are `fr`-based, not fixed widths: a fixed width keeps them pinned to their
   * own narrow band regardless of how wide the row grows, bunching every stat together at the
   * row's trailing edge instead of spreading across it.
   */
  protected readonly rowGridClass =
    'grid grid-cols-[minmax(0,2fr)_repeat(8,minmax(0,1fr))] items-center';

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
   * Resolves a match's map image, exposed to the template.
   */
  protected readonly mapImageUrl = resolveMapImageUrl;

  /**
   * Resolves a match's agent portrait, exposed to the template.
   */
  protected readonly agentImageUrl = resolveAgentImageUrl;

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
   * Formats a ValoQuests damage amount, grouped in the active language, exposed to the template.
   *
   * @param damage - Damage the match or the day was worth.
   * @returns The grouped amount, e.g. `"1 250"`.
   */
  protected formatDamageAmount(damage: number): string {
    return formatDamage(damage, this.translation.language());
  }

  /**
   * Explains the amount a match was worth: which coefficient the day's ladder applied to it, or
   * why it was worth nothing at all.
   *
   * Named rather than left to be guessed: two identical wins on the same evening routinely carry
   * different amounts, and the ladder is the only thing that tells them apart.
   *
   * @param match - The match the amount belongs to.
   * @returns The tooltip sentence.
   */
  protected damageExplanation(match: Match): string {
    return this.translation.translate(
      match.damageCoefficientPercent === 0
        ? 'playerProfile.matches.damage.unvalued'
        : 'playerProfile.matches.damage.coefficient',
      { percent: match.damageCoefficientPercent },
    );
  }

  /**
   * The reduced share a match kept, as a short visible mark, or `null` when there is nothing to
   * explain — a match paid in full, or one the ruleset never priced.
   *
   * Rendered beside the amount rather than left to {@link damageExplanation} alone: that sentence
   * lives in a tooltip, and a tooltip opens on hover or on focus, neither of which a thumb does.
   * Two identical wins on one evening carrying different amounts has to be readable without a
   * pointer.
   *
   * @param match - The match the amount belongs to.
   * @returns The share as text, or `null` when the amount needs no qualifier.
   */
  protected damageShareLabel(match: Match): string | null {
    const percent = match.damageCoefficientPercent;

    return percent <= 0 || percent >= 100
      ? null
      : this.translation.translate('playerProfile.matches.damage.share', { percent });
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
   * Spells a season's raw code out in the active language.
   *
   * @param season - The season to label.
   * @returns The label to show in the filters.
   */
  private seasonName(season: Season): string {
    return formatSeasonName(season.name, (key, params) => this.translation.translate(key, params));
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
  /**
   * Colour of the share the next match keeps.
   *
   * Amber at full value, muted once the ladder has started taking a cut: the band is a warning
   * only from the moment there is something to warn about, and a red would overstate a rule that
   * still pays half.
   *
   * @param percent - Share the next match would keep.
   * @returns The Tailwind text colour utility.
   */
  protected yieldToneClass(percent: number): string {
    return percent >= 100 ? 'text-brand-500' : 'text-text-secondary';
  }
  /**
   * Switches half of the profile, and mirrors the choice into the URL.
   *
   * Replaces the current entry rather than pushing one: flipping between the two halves of a page
   * is not a step a reader wants to walk back through, and pushing would make Back mean "the other
   * tab" instead of "the page before this one".
   *
   * @param tab - The half to show.
   */
  protected onProfileTabChange(tab: 'COLONY' | 'VALORANT'): void {
    this.profileTab.set(tab);
    void this.router.navigate([], {
      queryParams: { tab: tab === 'VALORANT' ? null : tab.toLowerCase() },
      queryParamsHandling: 'merge',
      relativeTo: this.route,
      replaceUrl: true,
    });
  }
}

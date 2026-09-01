import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { PageHeader } from '@layout/page-header/page-header';
import { Tooltip } from '@shared/tooltip/tooltip';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  LucideCheck,
  LucideChevronDown,
  LucideChevronLeft,
  LucideChevronRight,
  LucideChevronUp,
} from '@lucide/angular';
import { interval } from 'rxjs';

import { ChallengeIconView } from '@shared/challenge-icon-view/challenge-icon-view';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { COUNTDOWN_REFRESH_INTERVAL_MS } from '@core/date/countdown.constants';
import { formatWeekdayDayMonth } from '@core/date/date-time.utils';
import { formatDateRange, RemainingTime, remainingWeekTime } from '@core/date/week-period.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { PlayersApi } from '@core/players/players-api';
import { CampaignRanking } from '@core/ranking/campaign-ranking';
import { RankingApi } from '@core/ranking/ranking-api';
import { resolveChampionPlayerId } from '@core/ranking/ranking-champion.utils';
import { RankingHistoryWeek } from '@core/ranking/ranking.model';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { Breakpoint } from '@core/viewport/breakpoint';
import { Podium } from '@pages/overview/podium/podium';
import { Avatar } from '@shared/avatar/avatar';
import { ChallengeRing } from '@shared/challenge-ring/challenge-ring';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { PositionBadge } from '@shared/position-badge/position-badge';
import { ProgressBar } from '@shared/progress-bar/progress-bar';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SKELETON_ROWS } from '@shared/resource-state/skeleton.constants';
import { WeekCountdown } from '@shared/week-countdown/week-countdown';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';
import { RankingColumn, RankingRow, RankingScope } from './leaderboard.model';
import {
  resolveRankingCells,
  resolveRankingColumns,
  resolveRequestedScope,
} from './leaderboard.utils';

/**
 * Weekly leaderboard page — the squad's tactical matrix.
 *
 * Crosses the seven tracked players with the five challenges drawn for the active week: one row
 * per player, one column per challenge, so who is carrying the week and which challenge the squad
 * is collectively stuck on are both readable in one glance. Reuses the challenge color language
 * from the quest page, so a tier means the same thing on both screens.
 *
 * This is the one place the weekly ranking is browsed, live week and closed weeks alike: the same
 * rows step back through `RankingApi.history` week by week. The campaign's boss drawer still shows
 * a week's damage in the context of its own fight, and links here for the full board.
 *
 * A closed week is a shorter row than the live one, and deliberately so: the backend freezes the
 * totals of a finalized week but not the per-challenge breakdown behind them, so those five columns
 * give way to the two figures it does keep — challenges cleared and days played.
 *
 * The same rows are read at three scales, picked with the scope switch above the board. They are one
 * competition, not three boards, and the scope only decides which figures a row can honestly carry:
 *
 * - **Day** ranks tonight on match damage alone, and adds the gap with yesterday — the answer to "did
 *   we have a good evening?". The bonuses and the materials are settled on the week and simply do not
 *   exist at this scale, so their columns are dropped rather than filled with dashes.
 * - **Week** is the board above, podium and week arrows included, and the only scale where the whole
 *   row has a value.
 * - **Campaign** sums the run in progress and replaces the day's gap with the number of the run's
 *   fights the player put damage into. No podium: a run is crowned when it ends.
 */
@Component({
  selector: 'app-leaderboard',
  imports: [
    TranslatePipe,
    NgTemplateOutlet,
    RouterLink,
    ChallengeIconView,
    Tooltip,
    Avatar,
    ChallengeRing,
    ChampionBadge,
    PositionBadge,
    ProgressBar,
    ResourceState,
    WeekCountdown,
    LucideCheck,
    LucideChevronDown,
    LucideChevronLeft,
    LucideChevronRight,
    LucideChevronUp,
    PageHeader,
    Podium,
  ],
  templateUrl: './leaderboard.html',
  host: { class: PAGE_LAYOUT_CLASS },
  providers: [CampaignRanking],
})
export class Leaderboard {
  /**
   * Data-access service backing the shared current-ranking resource.
   */
  private readonly rankingApi = inject(RankingApi);

  /**
   * Data-access service backing the shared current-challenges resource, used to resolve each
   * column's icon and color treatment from its difficulty tier.
   */
  private readonly challengesApi = inject(ChallengesApi);

  /**
   * Data-access service backing the shared roster resource, read only to put a face on a closed
   * week's rows: a finalized ranking entry carries a player id and a name, never a portrait.
   */
  private readonly playersApi = inject(PlayersApi);

  /**
   * Read model folding the run in progress into one line per player — the campaign scope's board.
   */
  private readonly campaignRanking = inject(CampaignRanking);

  /**
   * i18n service used to resolve each challenge's translated category label, and read for the
   * active language when grouping damage amounts.
   */
  private readonly translation = inject(Translation);

  /**
   * The activated route, read for the two things a link may ask this page to open on: a closed week
   * and a scope.
   */
  private readonly route = inject(ActivatedRoute);

  /**
   * Week asked for in the URL as `?week=YYYY-MM-DD`, or `null` when the page was opened plain.
   *
   * Read once from the snapshot rather than followed reactively: this is a deep link into a closed
   * week — the campaign's boss drawer is the one place that writes it — and the arrows take over
   * from the moment the visitor uses them.
   */
  private readonly requestedWeekStart = this.route.snapshot.queryParamMap.get('week');

  /**
   * Whether the viewport can hold the matrix layout: below it, the same rows are rendered as
   * cards. Only the matching layout is put in the DOM, never both.
   */
  protected readonly isWide = inject(Breakpoint).isWide;

  /**
   * Current time, refreshed periodically to keep the countdown display accurate.
   */
  private readonly now = signal(new Date());

  /**
   * Reactive resource fetching the current week's ranking.
   */
  protected readonly rankingResource = this.rankingApi.current;

  /**
   * Reactive resource fetching the current week's challenges, shared with the overview hero and
   * the challenges page.
   */
  private readonly challengesResource = this.challengesApi.current;

  /**
   * Reactive resource fetching today's board, and yesterday's figures to hold it against.
   */
  private readonly dailyResource = this.rankingApi.daily;

  /**
   * Stretch of time the board is ranking. The week is the default: it is the competition proper,
   * the one the rollover and the champion title are settled on.
   *
   * A caller may ask for another one as `?scope=day`, the way the accueil's own day preview links
   * here — a link naming a board must open on that board. Read once from the snapshot, like
   * {@link requestedWeekStart}: the switch takes over from the first press.
   */
  protected readonly scope = signal<RankingScope>(
    resolveRequestedScope(this.route.snapshot.queryParamMap.get('scope')),
  );

  /**
   * The three scopes, in the order the switch offers them — shortest first, so the control reads as
   * a zoom level rather than as an unordered set of filters.
   */
  protected readonly scopes: readonly RankingScope[] = ['DAY', 'WEEK', 'CAMPAIGN'];

  /**
   * State of the resources the week scope reads — the board's own, and the week's challenges the
   * five columns are drawn from.
   */
  private readonly weekLoading = anyLoading(this.rankingResource, this.challengesResource);
  private readonly weekError = anyError(this.rankingResource, this.challengesResource);

  /**
   * State of the day scope's single resource.
   */
  private readonly dayLoading = anyLoading(this.dailyResource);
  private readonly dayError = anyError(this.dailyResource);

  /**
   * Whether a backing resource of the *active scope* is still loading.
   *
   * Scoped rather than combined: the day board must not wait on the ten weeks of history the
   * campaign board sums, and the campaign board must not wait on the week's challenge catalogue it
   * has no column for.
   */
  protected readonly isLoading = computed(() => {
    switch (this.scope()) {
      case 'DAY':
        return this.dayLoading();
      case 'CAMPAIGN':
        return this.campaignRanking.isLoading();
      default:
        return this.weekLoading();
    }
  });

  /**
   * Whether a backing resource of the active scope failed to load.
   */
  protected readonly hasError = computed(() => {
    switch (this.scope()) {
      case 'DAY':
        return this.dayError();
      case 'CAMPAIGN':
        return this.campaignRanking.hasError();
      default:
        return this.weekError();
    }
  });

  /**
   * The active week as the ranking describes it, or `null` until it has loaded.
   */
  private readonly currentWeek = computed(() => resourceValue(this.rankingResource, null) ?? null);

  /**
   * Every finalized week, most recent first — the weeks the arrows step back through.
   *
   * Deliberately outside {@link isLoading} and {@link hasError}: this screen opens on the live
   * week, and holding that behind a hundred weeks of history would trade the first paint for a
   * control most visits never touch. A failed history request costs the arrows and nothing else.
   */
  private readonly historyWeeks = computed<readonly RankingHistoryWeek[]>(
    () => resourceValue(this.rankingApi.history, null)?.content ?? [],
  );

  /**
   * Week the visitor stepped to with the arrows, or `null` while they have not touched them.
   */
  private readonly weekIndexOverride = signal<number | null>(null);

  /**
   * Index of the week on screen: `0` is the live week, `1` the one that closed most recently, and
   * so on back through {@link historyWeeks}.
   */
  protected readonly weekIndex = computed<number>(() => {
    const override = this.weekIndexOverride();
    if (override !== null) {
      return override;
    }

    const requested = this.requestedWeekStart;
    if (requested === null) {
      return 0;
    }

    // Falls back to the live week rather than to an error: a `?week=` pointing at a week that was
    // never finalized is a stale link, and the live board is the right thing to land on.
    const index = this.historyWeeks().findIndex((week) => week.weekStart === requested);
    return index === -1 ? 0 : index + 1;
  });

  /**
   * Whether the week on screen is the one still being played.
   */
  protected readonly isLiveWeek = computed(() => this.weekIndex() === 0);

  /**
   * Whether the board is on the week scope — the only one the arrows, the podium and the five
   * challenge columns belong to.
   */
  protected readonly isWeekScope = computed(() => this.scope() === 'WEEK');

  /**
   * Whether the board is on the day scope, which is the one scope carrying the gap with yesterday.
   */
  protected readonly isDayScope = computed(() => this.scope() === 'DAY');

  /**
   * Whether the board is on the campaign scope, which trades that gap for the fights joined.
   */
  protected readonly isCampaignScope = computed(() => this.scope() === 'CAMPAIGN');

  /**
   * The finalized week on screen, or `null` while the live one is.
   */
  private readonly selectedHistoryWeek = computed<RankingHistoryWeek | null>(() =>
    this.isLiveWeek() ? null : (this.historyWeeks()[this.weekIndex() - 1] ?? null),
  );

  /**
   * Boundaries of the week on screen, whichever of the two it is.
   */
  private readonly selectedWeek = computed<{ weekStart: string; weekEnd: string } | null>(
    () => this.selectedHistoryWeek() ?? this.currentWeek(),
  );

  /**
   * Dates the week on screen spans, e.g. `"18/08 – 24/08"`, so a week number resolves to real days.
   */
  protected readonly weekRangeLabel = computed<string>(() => {
    const week = this.selectedWeek();
    return week === null ? '' : formatDateRange(week.weekStart, week.weekEnd);
  });

  /**
   * Monday of the week on screen, or `null` while it has not loaded — the same `?week=` value the
   * campaign's own boss drawer already reads, so its "back to the campaign" link and this page's
   * `?week=` entry point stay two directions of one deep link.
   */
  protected readonly selectedWeekStart = computed<string | null>(
    () => this.selectedWeek()?.weekStart ?? null,
  );

  /**
   * Whether there is an older week to step back to.
   */
  protected readonly hasOlderWeek = computed(() => this.weekIndex() < this.historyWeeks().length);

  /**
   * Whether there is a more recent week to step forward to.
   */
  protected readonly hasNewerWeek = computed(() => this.weekIndex() > 0);

  /**
   * Time left before the weekly rollover, or `null` while loading. Same countdown as the overview
   * and quest pages, since it is the deadline all three screens are counting down to.
   */
  protected readonly remaining = computed<RemainingTime | null>(() => {
    const currentWeek = this.currentWeek();
    return currentWeek === null ? null : remainingWeekTime(currentWeek.weekEnd, this.now());
  });

  /**
   * Challenges selected for the active week, paired with their resolved icon and color treatment,
   * used both as table columns and to resolve each row's per-challenge cell visual.
   *
   * Empty on a closed week: the five challenges here are *this* week's draw, and hanging last
   * March's rows under them would cross two different weeks in one table.
   */
  protected readonly columns = computed<readonly RankingColumn[]>(() =>
    resolveRankingColumns(
      this.isWeekScope() && this.isLiveWeek()
        ? (resourceValue(this.challengesResource, null)?.challenges ?? [])
        : [],
      (key) => this.translation.translate(key),
      this.translation.language(),
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
   * Materials one clear of each of the week's challenges is worth, keyed by challenge id — the
   * live week's own catalogue, read once here rather than per row.
   */
  private readonly challengeMaterialsById = computed<ReadonlyMap<number, number>>(() => {
    const challenges = resourceValue(this.challengesResource, null)?.challenges ?? [];

    return new Map(challenges.map((challenge) => [challenge.id, challenge.materials]));
  });

  /**
   * The week on screen, as rows — the live board or a closed one, in the same shape either way so
   * both layouts render one kind of row rather than branching all the way down.
   */
  protected readonly rows = computed<readonly RankingRow[]>(() => {
    switch (this.scope()) {
      case 'DAY':
        return this.dailyRows();
      case 'CAMPAIGN':
        return this.campaignRows();
      default:
        return this.isLiveWeek() ? this.liveRows() : this.finalizedRows();
    }
  });

  /**
   * Rows of players currently in the campaign — the table proper.
   */
  protected readonly activeRows = computed(() => this.rows().filter((row) => row.inCampaign));

  /**
   * Rows of players out of the campaign, as a group of their own rather than a fade on the same
   * list (see root `CLAUDE.md`, `PlayerStatus`, and design-review.md §A8): they play and clear
   * challenges individually, but never occupy a ranking slot or bank materials for the colony,
   * which is a different *kind* of row, not a lesser one.
   */
  protected readonly outOfCampaignRows = computed(() =>
    this.rows().filter((row) => !row.inCampaign),
  );

  /**
   * Whether the live week has genuinely seen no play yet — every in-campaign player still sits at
   * zero (design-review.md's G4: this reads as "nobody has played" rather than as six identical
   * rows the visitor has to notice are all zero themselves).
   *
   * `false` on a closed week: a finalized week with nothing in it is the generic empty state
   * instead, since a week that closed without a single point played is not the case this reads.
   */
  protected readonly weekHasNoActivity = computed(() => {
    if (!this.isWeekScope() || !this.isLiveWeek()) {
      return false;
    }

    const entries = (this.currentWeek()?.ranking ?? []).filter((entry) => entry.position != null);
    return entries.length > 0 && entries.every((entry) => entry.totalDamage === 0);
  });

  /**
   * Live ranking entries mapped to display-ready rows: one cell per column, aligned by challenge id.
   */
  private readonly liveRows = computed<readonly RankingRow[]>(() => {
    const columns = this.columns();
    const championPlayerId = this.championPlayerId();
    const language = this.translation.language();
    const materialsById = this.challengeMaterialsById();
    return (this.currentWeek()?.ranking ?? []).map((entry) => {
      const cells = resolveRankingCells(columns, entry.challengeProgress, language);

      const bonus = entry.regularityBonus + entry.teamBonus;
      const materials = entry.challengeProgress
        .filter((progress) => progress.completed)
        .reduce((sum, progress) => sum + (materialsById.get(progress.challengeId) ?? 0), 0);

      return {
        // The backend omits `position` entirely from the JSON payload when null (global
        // non-null serialization), so the parsed value is `undefined`, not `null` - normalized
        // here so every `=== null` check downstream (component and template) is reliable.
        position: entry.position ?? null,
        positionVariation: entry.positionVariation,
        playerId: entry.player.id,
        displayName: entry.player.displayName,
        avatarUrl: resolvePlayerAvatarUrl(entry.player.portrait),
        // `totalDamage`, not `challengeDamage`: the ranking is ordered on the total, so showing
        // anything else next to a position would not explain the order it is in. `null` for an
        // out-of-campaign player: the figure does not exist for them, it is not merely zero — they
        // never deal boss damage in the first place (root `CLAUDE.md`, `PlayerStatus`).
        damageLabel: entry.position != null ? formatDamage(entry.totalDamage, language) : null,
        bonusLabel:
          entry.position != null && bonus !== 0 ? `+${formatDamage(bonus, language)}` : null,
        cells,
        isChampion: entry.player.id === championPlayerId,
        // The five cells above already say, challenge by challenge, what these two would summarize.
        completedLabel: null,
        activeDaysLabel: null,
        materialsLabel:
          entry.position != null && materials !== 0
            ? `+${formatDamage(materials, language)}`
            : null,
        inCampaign: entry.position != null,
        damageVariation: null,
        bossCountLabel: null,
      };
    });
  });

  /**
   * Today's rows: what each player brought in tonight, and the gap with last night.
   *
   * The shortest row of the three, because a day is the scale at which almost nothing is settled.
   * Only match damage exists — the challenge damage, the two bonuses and the materials are all
   * awarded on the week — so the bonus and materials figures are left `null` rather than printed as
   * a column of zeros claiming a value that has not been decided yet.
   *
   * The five challenge cells are dropped for the same reason: they measure progress toward a
   * *weekly* target, and hanging them off a day's row would read as a day's worth of progress.
   */
  private readonly dailyRows = computed<readonly RankingRow[]>(() => {
    const championPlayerId = this.championPlayerId();
    const language = this.translation.language();
    const portraits = new Map(
      this.playersApi.players.value().map((player) => [player.id, player.portrait]),
    );

    return (resourceValue(this.dailyResource, null)?.ranking ?? []).map((entry) => ({
      position: entry.position ?? null,
      // Nothing to compare against: the arrows beside a position mean "since last week", and a day
      // has no such movement stored. The gap this scope does answer is on the damage, not the rank.
      positionVariation: 0,
      playerId: entry.playerId,
      displayName: entry.displayName,
      avatarUrl: resolvePlayerAvatarUrl(entry.portrait ?? portraits.get(entry.playerId) ?? null),
      // Zero is a real answer at this scale — the evening somebody sat out — so it is printed rather
      // than dashed. Out-of-campaign players keep the dash, as on every other scope.
      damageLabel: entry.position != null ? formatDamage(entry.matchDamage, language) : null,
      bonusLabel: null,
      cells: [],
      completedLabel: null,
      activeDaysLabel: null,
      isChampion: entry.playerId === championPlayerId,
      materialsLabel: null,
      inCampaign: entry.position != null,
      damageVariation: entry.position != null ? entry.damageVariation : null,
      bossCountLabel: null,
    }));
  });

  /**
   * The run in progress, one row per player.
   *
   * Every figure is a sum over the weeks played so far, so all four are real numbers rather than the
   * live week's mix of live and frozen ones. What replaces the day's gap is the count of the run's
   * fights the player put damage into — the campaign's own version of "did you turn up?".
   */
  private readonly campaignRows = computed<readonly RankingRow[]>(() => {
    const championPlayerId = this.championPlayerId();
    const language = this.translation.language();
    const portraits = new Map(
      this.playersApi.players.value().map((player) => [player.id, player.portrait]),
    );

    return this.campaignRanking.entries().map((entry) => ({
      position: entry.position,
      positionVariation: 0,
      playerId: entry.playerId,
      displayName: entry.displayName,
      avatarUrl: resolvePlayerAvatarUrl(portraits.get(entry.playerId) ?? null),
      damageLabel: entry.position != null ? formatDamage(entry.totalDamage, language) : null,
      bonusLabel:
        entry.position != null && entry.bonus !== 0
          ? `+${formatDamage(entry.bonus, language)}`
          : null,
      cells: [],
      completedLabel: `${entry.completedChallenges}`,
      activeDaysLabel: `${entry.activeDays}`,
      isChampion: entry.playerId === championPlayerId,
      // Same reason as a closed week: a run's past weeks no longer carry the catalogue their
      // materials value would have to be read off.
      materialsLabel: null,
      inCampaign: entry.position != null,
      damageVariation: null,
      bossCountLabel: `${entry.bossCount}`,
    }));
  });

  /**
   * A closed week's frozen entries, mapped to the same rows the live board uses.
   *
   * Shorter by necessity rather than by choice: a finalized week keeps its totals and nothing of
   * the progress behind them, so the per-challenge cells give way to the two figures it does keep.
   */
  private readonly finalizedRows = computed<readonly RankingRow[]>(() => {
    const week = this.selectedHistoryWeek();
    if (week === null) {
      return [];
    }

    const language = this.translation.language();
    // A finalized entry carries an id and a name, never a portrait: the roster is where the face
    // comes from, exactly as `BossCampaign` resolves it for the boss drawer's own contributions.
    const portraits = new Map(
      this.playersApi.players.value().map((player) => [player.id, player.portrait]),
    );

    return week.ranking.map((entry) => {
      const bonus = entry.regularityBonus + entry.teamBonus;

      return {
        position: entry.position ?? null,
        // Nothing to compare against: the arrows on the live board mean "since last week", and a
        // closed week's own movement was never stored.
        positionVariation: 0,
        playerId: entry.playerId,
        displayName: entry.displayName,
        avatarUrl: resolvePlayerAvatarUrl(portraits.get(entry.playerId) ?? null),
        damageLabel: entry.position != null ? formatDamage(entry.totalDamage, language) : null,
        bonusLabel:
          entry.position != null && bonus !== 0 ? `+${formatDamage(bonus, language)}` : null,
        cells: [],
        // On a closed week the title goes to whoever actually won it: this is the week that handed
        // it out, not a week decorated by whoever holds it today.
        isChampion: entry.position === 1,
        completedLabel: `${entry.completedChallenges}`,
        activeDaysLabel: `${entry.activeDays}`,
        // Nothing to sum: a finalized week's challenge catalogue (and each challenge's materials
        // value) is not fetched, only its frozen totals — same reason `cells` is empty above.
        materialsLabel: null,
        inCampaign: entry.position != null,
        damageVariation: null,
        bossCountLabel: null,
      };
    });
  });

  /**
   * The day on the board, spelled out (`"Mardi 01/09"`), or `""` while it has not loaded.
   *
   * The backend's own day, not one computed here: it is resolved against the rollover timezone, and
   * a reader whose clock has already crossed midnight must still be told which evening they are
   * looking at.
   */
  protected readonly dayLabel = computed<string>(() => {
    const daily = resourceValue(this.dailyResource, null) ?? null;
    return daily === null ? '' : formatWeekdayDayMonth(daily.day, this.translation.language());
  });

  /**
   * How much of the squad played on the day shown, as `{ played, roster }`, or `null` while the day
   * has not loaded.
   */
  protected readonly dayTurnout = computed<{ played: number; roster: number } | null>(() => {
    const daily = resourceValue(this.dailyResource, null) ?? null;
    return daily === null
      ? null
      : { played: daily.playedPlayerCount, roster: daily.rosterPlayerCount };
  });

  /**
   * Which week of the run the campaign board stops at, as `{ index, count }`, or `null` while the
   * run's bounds are unknown — the campaign scope's answer to the week arrows' date range.
   */
  protected readonly campaignProgress = computed<{ index: number; count: number } | null>(() => {
    const index = this.campaignRanking.runWeekIndex();
    const count = this.campaignRanking.runWeekCount();
    return index === null || count === null ? null : { index, count };
  });

  /**
   * Whether the board carries a bonus column. Every scope but the day: the regularity and team
   * bonuses are awarded on the week, so a day has none rather than none yet.
   */
  protected readonly showBonusColumn = computed(() => !this.isDayScope());

  /**
   * Whether the board carries the slot the materials figure sits in — filled on the live week, held
   * open as a blank on a closed one so the two weekly boards keep the same column rhythm.
   */
  protected readonly showMaterialsSlot = computed(() => this.isWeekScope());

  /**
   * Whether the board carries the two summary figures — challenges cleared and days played.
   *
   * A closed week and a run both have them, and for the same reason: neither can show the five
   * per-challenge cells, one because the breakdown was never frozen, the other because ten weeks of
   * draws do not fit five columns.
   */
  protected readonly showTotalsColumns = computed(
    () => this.isCampaignScope() || (this.isWeekScope() && !this.isLiveWeek()),
  );

  /**
   * Grid template the header and every row share, so a column heading always lands on its figures.
   *
   * Held here rather than inlined twice in the template: the two used to drift apart on every column
   * added, and a header off by one column is a board that lies without looking broken.
   */
  protected readonly gridClass = computed<string>(() => {
    switch (this.scope()) {
      case 'DAY':
        return 'grid-cols-[4rem_minmax(12rem,2fr)_minmax(7rem,0.6fr)_minmax(7rem,0.6fr)]';
      case 'CAMPAIGN':
        return 'grid-cols-[4rem_minmax(12rem,1.6fr)_6rem_7.5rem_repeat(3,minmax(5.5rem,0.5fr))]';
      default:
        return this.isLiveWeek()
          ? 'grid-cols-[4rem_minmax(12rem,1.6fr)_6rem_7.5rem_6rem_repeat(5,minmax(3.25rem,0.45fr))]'
          : 'grid-cols-[4rem_minmax(12rem,1.6fr)_6rem_7.5rem_2rem_repeat(2,minmax(5.5rem,0.5fr))]';
    }
  });

  /**
   * Placeholder line widths driving the loading skeleton.
   */
  protected readonly skeletonRows = SKELETON_ROWS;

  /**
   * Refreshes {@link now} every minute so the countdown stays accurate for the lifetime of the
   * page.
   */
  constructor() {
    interval(COUNTDOWN_REFRESH_INTERVAL_MS)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.now.set(new Date()));
  }

  /**
   * Steps the board to another week.
   *
   * @param offset - `1` to go one week further back, `-1` to come one week forward. Out-of-range
   *   steps are ignored rather than clamped, since the arrows are already disabled at both ends.
   */
  protected stepWeek(offset: number): void {
    const target = this.weekIndex() + offset;
    if (target < 0 || target > this.historyWeeks().length) {
      return;
    }

    this.weekIndexOverride.set(target);
  }

  /**
   * Switches the board to another scale.
   *
   * The week the arrows stepped to is deliberately kept rather than reset: stepping back three
   * weeks, glancing at the run, then coming back to find the arrows had rewound to today would undo
   * work the visitor did on purpose.
   *
   * @param scope - The scale to rank on.
   */
  protected selectScope(scope: RankingScope): void {
    this.scope.set(scope);
  }

  /**
   * Reloads the backing resources after a failure.
   *
   * Every scope's resources are retried, not only the active one's: {@link hasError} reports one
   * scope's combined state and cannot tell which request inside it failed, and the switch above the
   * board is one click away from asking for any of the others.
   */
  protected reload(): void {
    reloadAll(this.rankingResource, this.challengesResource, this.dailyResource);
    this.campaignRanking.reload();
  }
}

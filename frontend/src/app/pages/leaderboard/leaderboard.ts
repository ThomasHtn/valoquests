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
import {
  resolveChallengeMetricLabel,
  resolveChallengeVisual,
} from '@core/challenges/challenge-visual.utils';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { COUNTDOWN_REFRESH_INTERVAL_MS } from '@core/date/countdown.constants';
import {
  formatDateRange,
  isoWeekNumber,
  RemainingTime,
  remainingWeekTime,
} from '@core/date/week-period.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { PlayersApi } from '@core/players/players-api';
import { RankingApi } from '@core/ranking/ranking-api';
import { resolveChampionPlayerId } from '@core/ranking/ranking-champion.utils';
import { RankingHistoryWeek } from '@core/ranking/ranking.model';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { Breakpoint } from '@core/viewport/breakpoint';
import { Avatar } from '@shared/avatar/avatar';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { PositionBadge } from '@shared/position-badge/position-badge';
import { ProgressBar } from '@shared/progress-bar/progress-bar';
import { ProgressCircle } from '@shared/progress-circle/progress-circle';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SKELETON_ROWS } from '@shared/resource-state/skeleton.constants';
import { WeekCountdown } from '@shared/week-countdown/week-countdown';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';
import { RankingCell, RankingColumn, RankingRow } from './leaderboard.model';
import {
  buildCurrentValueLabel,
  buildTargetValueLabel,
  computeCompletionPercentage,
  formatMetricValue,
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
    ChampionBadge,
    PositionBadge,
    ProgressBar,
    ProgressCircle,
    ResourceState,
    WeekCountdown,
    LucideCheck,
    LucideChevronDown,
    LucideChevronLeft,
    LucideChevronRight,
    LucideChevronUp,
    PageHeader,
  ],
  templateUrl: './leaderboard.html',
  host: { class: PAGE_LAYOUT_CLASS },
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
   * i18n service used to resolve each challenge's translated category label, and read for the
   * active language when grouping damage amounts.
   */
  private readonly translation = inject(Translation);

  /**
   * Week asked for in the URL as `?week=YYYY-MM-DD`, or `null` when the page was opened plain.
   *
   * Read once from the snapshot rather than followed reactively: this is a deep link into a closed
   * week — the campaign's boss drawer is the one place that writes it — and the arrows take over
   * from the moment the visitor uses them.
   */
  private readonly requestedWeekStart = inject(ActivatedRoute).snapshot.queryParamMap.get('week');

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
   * Whether either backing resource is still loading.
   */
  protected readonly isLoading = anyLoading(this.rankingResource, this.challengesResource);

  /**
   * Whether either backing resource failed to load.
   */
  protected readonly hasError = anyError(this.rankingResource, this.challengesResource);

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
   * ISO number of the week on screen, or `null` while it has not loaded.
   */
  protected readonly weekNumber = computed<number | null>(() => {
    const week = this.selectedWeek();
    return week === null ? null : isoWeekNumber(week.weekStart);
  });

  /**
   * Dates the week on screen spans, e.g. `"18/08 – 24/08"`, so a week number resolves to real days.
   */
  protected readonly weekRangeLabel = computed<string>(() => {
    const week = this.selectedWeek();
    return week === null ? '' : formatDateRange(week.weekStart, week.weekEnd);
  });

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
    (this.isLiveWeek() ? (resourceValue(this.challengesResource, null)?.challenges ?? []) : []).map(
      (challenge) => ({
        challengeId: challenge.id,
        name: challenge.name,
        categoryLabel: resolveChallengeMetricLabel(challenge.metric, (key) =>
          this.translation.translate(key),
        ),
        targetLabel: challenge.targetValue
          ? formatMetricValue(challenge.targetValue, this.translation.language())
          : null,
        tooltip: `${challenge.name} — ${challenge.description}`,
        visual: resolveChallengeVisual(challenge.metric, challenge.difficulty),
      }),
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
   * The week on screen, as rows — the live board or a closed one, in the same shape either way so
   * both layouts render one kind of row rather than branching all the way down.
   */
  protected readonly rows = computed<readonly RankingRow[]>(() =>
    this.isLiveWeek() ? this.liveRows() : this.finalizedRows(),
  );

  /**
   * Live ranking entries mapped to display-ready rows: one cell per column, aligned by challenge id.
   */
  private readonly liveRows = computed<readonly RankingRow[]>(() => {
    const columns = this.columns();
    const championPlayerId = this.championPlayerId();
    const language = this.translation.language();
    return (this.currentWeek()?.ranking ?? []).map((entry) => {
      const cells: RankingCell[] = columns.map((column) => {
        const progress = entry.challengeProgress.find(
          (candidate) => candidate.challengeId === column.challengeId,
        );
        return {
          challengeId: column.challengeId,
          name: column.name,
          categoryLabel: column.categoryLabel,
          currentValueLabel: buildCurrentValueLabel(progress, language),
          targetValueLabel: buildTargetValueLabel(progress, language),
          completionPercentage: computeCompletionPercentage(progress),
          completed: progress?.completed ?? false,
          visual: column.visual,
        };
      });

      const bonus = entry.regularityBonus + entry.teamBonus;

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
        // anything else next to a position would not explain the order it is in.
        damageLabel: formatDamage(entry.totalDamage, language),
        bonusLabel: bonus === 0 ? null : `+${formatDamage(bonus, language)}`,
        cells,
        isChampion: entry.player.id === championPlayerId,
        // The five cells above already say, challenge by challenge, what these two would summarize.
        completedLabel: null,
        activeDaysLabel: null,
      };
    });
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
        damageLabel: formatDamage(entry.totalDamage, language),
        bonusLabel: bonus === 0 ? null : `+${formatDamage(bonus, language)}`,
        cells: [],
        // On a closed week the title goes to whoever actually won it: this is the week that handed
        // it out, not a week decorated by whoever holds it today.
        isChampion: entry.position === 1,
        completedLabel: `${entry.completedChallenges}`,
        activeDaysLabel: `${entry.activeDays}`,
      };
    });
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
   * Reloads both backing resources after a failure.
   *
   * Both are retried because {@link hasError} reports their combined state and cannot tell which
   * one failed.
   */
  protected reload(): void {
    reloadAll(this.rankingResource, this.challengesResource);
  }
}

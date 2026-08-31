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
import { formatDateRange, RemainingTime, remainingWeekTime } from '@core/date/week-period.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { PlayersApi } from '@core/players/players-api';
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
import { RankingColumn, RankingRow } from './leaderboard.model';
import { resolveRankingCells, resolveRankingColumns } from './leaderboard.utils';

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
      this.isLiveWeek() ? (resourceValue(this.challengesResource, null)?.challenges ?? []) : [],
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
  protected readonly rows = computed<readonly RankingRow[]>(() =>
    this.isLiveWeek() ? this.liveRows() : this.finalizedRows(),
  );

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
    if (!this.isLiveWeek()) {
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

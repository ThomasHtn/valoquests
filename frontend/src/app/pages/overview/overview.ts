import { DecimalPipe } from '@angular/common';
import { Component, computed, signal, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { LucideHammer, LucideSwords, LucideUsers, LucideWheat } from '@lucide/angular';
import { interval } from 'rxjs';

import { ChallengesApi } from '@core/challenges/challenges-api';
import { ColonyView } from '@core/colony/colony-view';
import { ColonyPresenceState } from '@core/colony/colony.model';
import { COUNTDOWN_REFRESH_INTERVAL_MS } from '@core/date/countdown.constants';
import { formatDateRange, isoWeekNumber, remainingWeekTime } from '@core/date/week-period.utils';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { RankingApi } from '@core/ranking/ranking-api';
import { PageHeader } from '@layout/page-header/page-header';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';
import { WeekSummary } from './overview.model';
import { ConfrontationBand } from './confrontation-band/confrontation-band';
import { MiniRanking } from './mini-ranking/mini-ranking';
import { TownSilhouette } from './town-silhouette/town-silhouette';
import { WeekRecap } from './week-recap/week-recap';
import { CountUp } from '@shared/count-up/count-up';
import { Tooltip } from '@shared/tooltip/tooltip';

/**
 * Order the roster's presence tokens are grouped in, so the row reads as a gauge rather than as a
 * pattern with holes: counted first, then those who played short of the threshold, then the rest.
 */
const PRESENCE_ORDER: Record<ColonyPresenceState, number> = { FULL: 0, PARTIAL: 1, NONE: 2 };

/**
 * One presence token of the day's roster row: who it stands for, and how far into the day they got.
 */
interface PresenceSlot {
  /**
   * Stable identity for the `@for` track expression — the player's own id.
   */
  readonly key: string;

  /**
   * How far into the day the player got, which is what fills the hexagon.
   */
  readonly state: ColonyPresenceState;

  /**
   * Already-translated bubble naming the player and saying how their day went.
   */
  readonly tooltip: string;
}

/**
 * Accueil ("La colonie").
 *
 * Shown once inside the application shell (at `/overview`), it leads with the colony's waterfront —
 * the objective of the game, drawn as a place that grows with the ladder (see `TownSilhouette`)
 * rather than as a number in a hexagon. Under it the page stops being a stack of panels: the week is
 * one confrontation band (squad, clock, threat), the day is what today is worth beside who is ahead
 * on it, and the week's standing closes the page on a single line. Each block is named by a section
 * rule rather than framed by a card, which is what ended the nested-panel stacking the page had
 * grown.
 *
 * The day and the week used to be two boards side by side, which was the same three names twice and
 * both of them the leaderboard's own scopes one click away. What the accueil owes the day is what it
 * is worth, not a second podium.
 *
 * The colony's own economy and its tier ladder are `/campaign`'s to report: this page carries the
 * town, the state it is in and who is bringing it in. See design-review.md §3.1.
 */
@Component({
  selector: 'app-overview',
  imports: [
    TranslatePipe,
    ConfrontationBand,
    DecimalPipe,
    LucideHammer,
    LucideSwords,
    LucideUsers,
    LucideWheat,
    MiniRanking,
    PageHeader,
    RouterLink,
    CountUp,
    Tooltip,
    TownSilhouette,
    WeekRecap,
  ],
  templateUrl: './overview.html',
  styleUrl: './overview.css',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Overview {
  /**
   * The squad's shared colony — the town, the day's turnout and the week's harvest.
   */
  protected readonly colony = inject(ColonyView);

  /**
   * Data-access service backing the shared current-challenges resource, which also carries the
   * active week's boundaries used by the confrontation band's clock.
   */
  private readonly challengesApi = inject(ChallengesApi);

  /**
   * The day's board, read here only to know whether there is one — see {@link dayIsEmpty}. The
   * resource is shared, so this costs the page no second request.
   */
  private readonly rankingApi = inject(RankingApi);

  /**
   * i18n service, read for the language every figure on this page is grouped in.
   */
  private readonly translation = inject(Translation);

  /**
   * Current time, refreshed periodically to keep the countdown display accurate.
   */
  private readonly now = signal(new Date());

  /**
   * Active week's number and remaining time. The page owns the ticker for both the section rule's
   * week number and the band's clock, so one interval serves the whole screen.
   */
  protected readonly week = computed<WeekSummary | null>(() => {
    const currentChallenges = this.challengesApi.current;
    if (!currentChallenges.hasValue()) {
      return null;
    }

    const currentWeek = currentChallenges.value();

    return {
      number: isoWeekNumber(currentWeek.weekStart),
      dateRange: formatDateRange(currentWeek.weekStart, currentWeek.weekEnd),
      remaining: remainingWeekTime(currentWeek.weekEnd, this.now()),
    };
  });

  /**
   * Where the week sits in the run, `1 / 10`.
   *
   * The section used to be titled with the ISO week number, which read as "la semaine 35" right
   * under an eyebrow saying "semaine 1 sur 10" — two different numbers for the same week. The run's
   * own count is the one the whole page is scored on.
   */
  protected readonly runWeek = computed<{ index: number; count: number } | null>(() => {
    const colony = this.colony.colony();

    return colony === null ? null : { index: colony.runWeekIndex, count: colony.runWeekCount };
  });

  /**
   * One token per roster member, grouped by how far into the day they got.
   *
   * Sorted rather than left in roster order, and that is the whole reason the row reads: grouped, it
   * is a gauge with a lit run and an empty tail; scattered, it is a pattern with holes the reader has
   * to count. The order runs from "counted" through "played, short of the bar" to "has not played",
   * which is also the order a reader would rank them in.
   */
  protected readonly presenceSlots = computed<readonly PresenceSlot[]>(() =>
    [...this.colony.presencePips()]
      .sort((left, right) => PRESENCE_ORDER[left.state] - PRESENCE_ORDER[right.state])
      .map((pip) => ({ key: `${pip.playerId}`, state: pip.state, tooltip: pip.ariaLabel })),
  );

  /**
   * What the squad dealt today, already formatted — the term the day's conversion starts from.
   *
   * Summed over the ranked rows alone, which is the one thing that makes this figure honest: the
   * colony reads the day on `Player.COMPETITIVE_STATUS` (see `ColonyActivityReader`), so an inactive
   * player's matches never become food. Summing the whole board instead would print an amount on the
   * left of the rail that the amount on its right could not have been made from, and a conversion
   * whose two ends disagree is worse than no conversion at all.
   */
  protected readonly dayDamageLabel = computed<string>(() => {
    const daily = this.rankingApi.daily;
    const total = daily.hasValue()
      ? daily
          .value()
          .ranking.filter((entry) => entry.position != null)
          .reduce((sum, entry) => sum + entry.matchDamage, 0)
      : 0;

    return formatDamage(total, this.translation.language());
  });

  /**
   * Whether nobody has played today, which is what the "Aujourd'hui" block swaps its board for a
   * sentence on.
   *
   * Read off the day's own turnout rather than off the rows: the backend ranks the whole roster
   * from the first minute of the day, so a board that has loaded always has rows and they are all
   * at zero until somebody plays.
   *
   * `false` while the day is still loading, so the board renders its own waiting state instead of
   * the page flashing a call to play at a reader who may well have already played.
   */
  protected readonly dayIsEmpty = computed<boolean>(() => {
    const daily = this.rankingApi.daily;

    return daily.hasValue() && daily.value().playedPlayerCount === 0;
  });

  /**
   * The colony's own step, for the caption above the population figure.
   */
  protected readonly currentTier = computed(() =>
    this.colony.ladder().find((tier) => tier.state === 'CURRENT'),
  );

  /**
   * Refreshes {@link now} every minute so the countdown stays accurate for the lifetime of the
   * page.
   */
  constructor() {
    interval(COUNTDOWN_REFRESH_INTERVAL_MS)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.now.set(new Date()));
  }
}

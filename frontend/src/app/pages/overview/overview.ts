import { DecimalPipe } from '@angular/common';
import { Component, computed, signal, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { LucideHammer, LucideUsers } from '@lucide/angular';
import { interval } from 'rxjs';

import { ChallengesApi } from '@core/challenges/challenges-api';
import { ColonyView } from '@core/colony/colony-view';
import { COUNTDOWN_REFRESH_INTERVAL_MS } from '@core/date/countdown.constants';
import { formatDateRange, isoWeekNumber, remainingWeekTime } from '@core/date/week-period.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { PageHeader } from '@layout/page-header/page-header';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';
import { WeekSummary } from './overview.model';
import { ConfrontationBand } from './confrontation-band/confrontation-band';
import { MiniRanking } from './mini-ranking/mini-ranking';
import { TownSilhouette } from './town-silhouette/town-silhouette';
import { WeekRecap } from './week-recap/week-recap';
import { CountUp } from '@shared/count-up/count-up';

/**
 * Accueil ("La colonie").
 *
 * Shown once inside the application shell (at `/overview`), it leads with the colony's waterfront —
 * the objective of the game, drawn as a place that grows with the ladder (see `TownSilhouette`)
 * rather than as a number in a hexagon. Under it the page stops being a stack of panels: the week is
 * one confrontation band (squad, clock, threat), and the two boards close it side by side — the day
 * and the week, the same question asked at two scales. Each block is named by a section rule rather
 * than framed by a card, which is what ended the nested-panel stacking the page had grown.
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
    LucideUsers,
    MiniRanking,
    PageHeader,
    RouterLink,
    CountUp,
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

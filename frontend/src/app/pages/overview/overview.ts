import { Component, computed, signal, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { LucideHammer, LucideMagnet, LucideUsers, LucideWheat } from '@lucide/angular';
import { interval } from 'rxjs';

import { ChallengesApi } from '@core/challenges/challenges-api';
import { ColonyView } from '@core/colony/colony-view';
import { COUNTDOWN_REFRESH_INTERVAL_MS } from '@core/date/countdown.constants';
import { formatDateRange, isoWeekNumber, remainingWeekTime } from '@core/date/week-period.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { PageHeader } from '@layout/page-header/page-header';
import { BarChart } from '@shared/chart/bar-chart';
import { ChartBar } from '@shared/chart/chart.model';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';
import { WeekSummary } from './overview.model';
import { ConfrontationBand } from './confrontation-band/confrontation-band';
import { LadderStrip } from './ladder-strip/ladder-strip';
import { MiniRanking } from './mini-ranking/mini-ranking';
import { TownSilhouette } from './town-silhouette/town-silhouette';

/**
 * Accueil ("La colonie").
 *
 * Shown once inside the application shell (at `/overview`), it leads with the colony's waterfront —
 * the objective of the game, drawn as a place that grows with the ladder (see `TownSilhouette`)
 * rather than as a number in a hexagon. Under it the page stops being a stack of panels: the week is
 * one confrontation band (squad, clock, threat), the colony's economy and the week's ranking sit
 * side by side, and the ladder closes the page as the trail it is. Each block is named by a section
 * rule rather than framed by a card, which is what ended the nested-panel stacking the page had
 * grown. See design-review.md §3.1.
 */
@Component({
  selector: 'app-overview',
  imports: [
    TranslatePipe,
    BarChart,
    ConfrontationBand,
    LadderStrip,
    LucideHammer,
    LucideMagnet,
    LucideUsers,
    LucideWheat,
    MiniRanking,
    PageHeader,
    RouterLink,
    TownSilhouette,
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
   * The colony's own step, for the caption above the population figure.
   */
  protected readonly currentTier = computed(() =>
    this.colony.ladder().find((tier) => tier.state === 'CURRENT'),
  );

  /**
   * The food window's days, one bar each — today highlighted, days not yet lived drawn recessive.
   * Mirrors `Campaign`'s own food-this-week histogram.
   */
  protected readonly foodWeekBars = computed<readonly ChartBar[]>(() =>
    this.colony.foodDays().map((day) => ({
      label: day.weekdayInitial,
      value: day.harvestValue ?? 0,
      valueLabel: day.harvestLabel,
      detail: day.ariaLabel,
      highlighted: day.isToday,
      muted: day.isPlaceholder,
    })),
  );

  /**
   * Sr-only prose standing in for the food bars, each day's own accessible label read in sequence
   * — the canvas is one opaque image to assistive technology.
   */
  protected readonly foodWeekSummary = computed<string>(() =>
    this.colony
      .foodDays()
      .map((day) => day.ariaLabel)
      .join(', '),
  );

  /**
   * How long the harvest keeps and what today has brought in — the food cell's own second line.
   *
   * The window's length is read from the days the colony actually returned rather than written into
   * the sentence: `foodWindowDays` is the backend's rule, not this page's.
   */
  protected readonly foodWindowCaption = computed<string>(() => {
    const days = this.colony.foodDays();
    const today = days.find((day) => day.isToday);

    return this.translation.translate('overview.economy.foodWindow', {
      days: days.length,
      today: today?.harvestLabel ?? '0',
    });
  });

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

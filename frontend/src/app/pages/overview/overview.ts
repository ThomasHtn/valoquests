import { DecimalPipe } from '@angular/common';
import { Component, computed, signal, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { interval } from 'rxjs';

import { ChallengesApi } from '@core/challenges/challenges-api';
import { ColonyView } from '@core/colony/colony-view';
import { COUNTDOWN_REFRESH_INTERVAL_MS } from '@core/date/countdown.constants';
import { formatDateRange, isoWeekNumber, remainingWeekTime } from '@core/date/week-period.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { PageHeader } from '@layout/page-header/page-header';
import { BarChart } from '@shared/chart/bar-chart';
import { ChartBar } from '@shared/chart/chart.model';
import { BossFightCard } from '@shared/boss-fight-card/boss-fight-card';
import { ProgressBar } from '@shared/progress-bar/progress-bar';
import { WeekCountdown } from '@shared/week-countdown/week-countdown';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';
import { WeekSummary } from './overview.model';
import { MiniRanking } from './mini-ranking/mini-ranking';
import { TownSilhouette } from './town-silhouette/town-silhouette';

/**
 * Accueil ("La colonie").
 *
 * Shown once inside the application shell (at `/overview`), it leads with the town — the objective
 * of the game, drawn as a growing silhouette (see `TownSilhouette`) rather than a number in a
 * hexagon — and answers the day's one question, "what does tonight's game bring the colony". The
 * week's own fight and the week's ranking each get a compact summary linking out to their full
 * page: the fight reuses `/week`'s own `BossFightCard`, minus its rewards aside, which only fits
 * the fight's full-width home. See design-review.md §3.1.
 */
@Component({
  selector: 'app-overview',
  imports: [
    TranslatePipe,
    BarChart,
    BossFightCard,
    DecimalPipe,
    MiniRanking,
    PageHeader,
    ProgressBar,
    RouterLink,
    TownSilhouette,
    WeekCountdown,
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
   * active week's boundaries used by the header's countdown.
   */
  private readonly challengesApi = inject(ChallengesApi);

  /**
   * Current time, refreshed periodically to keep the countdown display accurate.
   */
  private readonly now = signal(new Date());

  /**
   * Active week's remaining time, or `null` while loading — the header countdown's only input now
   * that the eyebrow reads the campaign's own week count instead (see {@link ColonyView.eyebrow}).
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
   * The town's own step, for the headline caption above the population figure.
   */
  protected readonly currentTier = computed(() =>
    this.colony.ladder().find((tier) => tier.state === 'CURRENT'),
  );

  /**
   * The step the town is climbing towards, for the "prochain palier" row beside the silhouette.
   */
  protected readonly nextTier = computed(() => this.colony.ladder().find((tier) => tier.isNext));

  /**
   * The food window's `foodWindowDays` days, one bar each — today highlighted, days not yet lived
   * drawn recessive. Mirrors `Campaign`'s own food-this-week histogram.
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
   * Refreshes {@link now} every minute so the countdown stays accurate for the lifetime of the
   * page.
   */
  constructor() {
    interval(COUNTDOWN_REFRESH_INTERVAL_MS)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.now.set(new Date()));
  }
}

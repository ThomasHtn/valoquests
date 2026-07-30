import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LucideCalendar, LucideClock } from '@lucide/angular';
import { interval } from 'rxjs';

import { ChallengesApi } from '@core/challenges/challenges-api';
import { formatDateRange, isoWeekNumber, remainingWeekTime } from '@core/date/week-period.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { COUNTDOWN_REFRESH_INTERVAL_MS } from './overview.constants';
import { WeeklyChallenges } from './weekly-challenges/weekly-challenges';
import { WeeklyRanking } from './weekly-ranking/weekly-ranking';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';

/**
 * Overview page.
 *
 * Landing page shown at the application root route. Its header displays the active week and the
 * time remaining before it ends; its body hosts the weekly widgets, starting with the weekly
 * challenges card.
 */
@Component({
  selector: 'app-overview',
  imports: [TranslatePipe, WeeklyChallenges, WeeklyRanking, LucideCalendar, LucideClock],
  templateUrl: './overview.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Overview {
  /**
   * Data-access service backing the shared current-challenges resource, which also carries the
   * active week's boundaries used by the header.
   */
  private readonly challengesApi = inject(ChallengesApi);

  /**
   * Current time, refreshed periodically to keep the countdown display accurate.
   */
  private readonly now = signal(new Date());

  /**
   * Active week's number, date range and remaining time, or `null` while loading.
   */
  protected readonly week = computed(() => {
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
   * Refreshes {@link now} every minute so the countdown stays accurate for the lifetime of the page.
   */
  constructor() {
    interval(COUNTDOWN_REFRESH_INTERVAL_MS)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.now.set(new Date()));
  }
}

import { Component, computed, signal, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { LucideBookOpen } from '@lucide/angular';
import { interval } from 'rxjs';

import { ChallengesApi } from '@core/challenges/challenges-api';
import { formatDateRange, isoWeekNumber, remainingWeekTime } from '@core/date/week-period.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';
import { COUNTDOWN_REFRESH_INTERVAL_MS } from './overview.constants';
import { WeekSummary } from './overview.model';
import { BossEncounter } from './boss-encounter/boss-encounter';
import { Podium } from './podium/podium';
import { TeamProgress } from './team-progress/team-progress';

/**
 * Overview page.
 *
 * Landing page shown once inside the application shell (at `/overview`), presenting the week's
 * hero at a glance in a single, non-scrolling screen: the header (active week and countdown), the
 * weekly boss encounter, the team's collective progress toward the week's challenges, and the
 * ranking podium. The full challenge catalogue and the live player-by-player ranking table used to
 * be additional scroll-snapped sections of this same page; they are now their own routes
 * (`Challenges` at `/challenges`, `Leaderboard` at `/leaderboard`) since neither belongs in this
 * page's single-screen "Vue d'ensemble" mockup.
 */
@Component({
  selector: 'app-overview',
  imports: [TranslatePipe, RouterLink, BossEncounter, Podium, TeamProgress, LucideBookOpen],
  templateUrl: './overview.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Overview {
  /**
   * Data-access service backing the shared current-challenges resource, which also carries the
   * active week's boundaries used by the header and the hero.
   */
  private readonly challengesApi = inject(ChallengesApi);

  /**
   * Current time, refreshed periodically to keep the countdown display accurate.
   */
  private readonly now = signal(new Date());

  /**
   * Active week's number, date range and remaining time, or `null` while loading.
   *
   * Computed once here, from a single ticking clock, and passed down to `BossEncounter` and
   * `TeamProgress` so their countdown never drifts from this value or from the header's own.
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
   * Refreshes {@link now} every minute so the countdown stays accurate for the lifetime of the
   * page.
   */
  constructor() {
    interval(COUNTDOWN_REFRESH_INTERVAL_MS)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.now.set(new Date()));
  }
}

import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';

import { CurrentChallenges } from '@core/challenges/challenge.model';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { resolveChallengeVisual } from '@core/challenges/challenge-visual.utils';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { COUNTDOWN_REFRESH_INTERVAL_MS } from '@core/date/countdown.constants';
import {
  formatDayMonth,
  isoWeekNumber,
  nextWeekStart,
  RemainingTime,
  remainingWeekTime,
} from '@core/date/week-period.utils';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SKELETON_ROWS } from '@shared/resource-state/skeleton.constants';
import { SectionDivider } from '@shared/section-divider/section-divider';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';
import { ChallengeRow } from './challenges.model';

/**
 * Weekly quest page.
 *
 * Presents the five challenges drawn for the active week — one per difficulty tier — as a board of
 * cards stating what each one asks for and what it is worth against the week's boss. Who has
 * cleared what is deliberately *not* shown here: that is the squad matrix's job (`Leaderboard` at
 * `/leaderboard`), and repeating it turned every card into a second, weaker copy of that screen.
 */
@Component({
  selector: 'app-challenges',
  imports: [TranslatePipe, ResourceState, SectionDivider],
  templateUrl: './challenges.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Challenges {
  /**
   * Data-access service backing the shared current-challenges resource.
   */
  private readonly challengesApi = inject(ChallengesApi);

  /**
   * i18n service, read for the active language rather than for a lookup: the damage amounts are
   * grouped with the separator that language uses.
   */
  private readonly translation = inject(Translation);

  /**
   * Current time, refreshed periodically to keep the countdown display accurate.
   */
  private readonly now = signal(new Date());

  /**
   * Reactive resource fetching the current week's challenges, shared with the overview header.
   */
  protected readonly challengesResource = this.challengesApi.current;

  /**
   * Whether the backing resource is still loading.
   */
  protected readonly isLoading = anyLoading(this.challengesResource);

  /**
   * Whether the backing resource failed to load.
   */
  protected readonly hasError = anyError(this.challengesResource);

  /**
   * Placeholder line widths driving the loading skeleton.
   */
  protected readonly skeletonRows = SKELETON_ROWS;

  /**
   * The active week as the backend describes it, or `null` until it has loaded.
   */
  private readonly currentWeek = computed<CurrentChallenges | null>(
    () => resourceValue(this.challengesResource, null) ?? null,
  );

  /**
   * Active week's ISO number, or `null` while loading.
   */
  protected readonly weekNumber = computed<number | null>(() => {
    const currentWeek = this.currentWeek();
    return currentWeek === null ? null : isoWeekNumber(currentWeek.weekStart);
  });

  /**
   * Time left before the weekly rollover, or `null` while loading.
   */
  protected readonly remaining = computed<RemainingTime | null>(() => {
    const currentWeek = this.currentWeek();
    return currentWeek === null ? null : remainingWeekTime(currentWeek.weekEnd, this.now());
  });

  /**
   * Day the next five challenges are drawn, as `DD/MM`, or `null` while loading.
   */
  protected readonly nextDrawDate = computed<string | null>(() => {
    const currentWeek = this.currentWeek();
    return currentWeek === null ? null : formatDayMonth(nextWeekStart(currentWeek.weekEnd));
  });

  /**
   * The week's challenges, paired with their resolved tier and color treatment.
   */
  protected readonly rows = computed<readonly ChallengeRow[]>(() => {
    const language = this.translation.language();

    return (this.currentWeek()?.challenges ?? []).map((challenge) => ({
      id: challenge.id,
      name: challenge.name,
      description: challenge.description,
      difficulty: challenge.difficulty,
      damage: formatDamage(challenge.damage, language),
      visual: resolveChallengeVisual(challenge.metric, challenge.difficulty),
    }));
  });

  /**
   * Damage the squad would deal by clearing every challenge of the week.
   *
   * Summed here rather than read from the API: it is a total of amounts already on screen, stated
   * so the board's stakes are legible without adding them up by eye — not a statistic the backend
   * owns.
   */
  protected readonly totalDamage = computed<string>(() => {
    const total = (this.currentWeek()?.challenges ?? []).reduce(
      (sum, challenge) => sum + challenge.damage,
      0,
    );
    return formatDamage(total, this.translation.language());
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

  /**
   * Reloads the backing resource after a failure.
   */
  protected reload(): void {
    reloadAll(this.challengesResource);
  }
}

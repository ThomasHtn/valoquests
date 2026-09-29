import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { StreakPip } from './streak-gauge.model';
import { weekdayInitials } from './streak-gauge.utils';

/**
 * The week's attendance: seven hexagons from Monday to Sunday, then the bonus the streak earns.
 */
@Component({
  selector: 'app-streak-gauge',
  imports: [TranslatePipe],
  templateUrl: './streak-gauge.html',
  styleUrl: './streak-gauge.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StreakGauge {
  /**
   * The week from Monday to Sunday, one pip per day, or `null` when only the count is known.
   */
  public readonly week = input.required<readonly StreakPip[] | null>();

  /**
   * Days of the week played.
   */
  public readonly days = input.required<number>();

  /**
   * Streak bonus in percent.
   */
  public readonly bonusPercent = input.required<number>();

  private readonly translation = inject(Translation);

  /**
   * Initials written above the seven pips, Monday first.
   */
  protected readonly weekdays = computed(() => weekdayInitials(this.translation.language()));

  /**
   * The pips drawn: the week itself, or the count lit from the left when the days are unknown.
   */
  protected readonly pips = computed<readonly StreakPip[]>(
    () =>
      this.week() ??
      Array.from({ length: 7 }, (_, index) => (index < this.days() ? 'played' : 'missed')),
  );
}

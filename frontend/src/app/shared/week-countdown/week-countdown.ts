import { Component, computed, inject, input } from '@angular/core';
import { LucideHourglass } from '@lucide/angular';

import { RemainingTime } from '@core/date/date.model';
import { Translation } from '@core/i18n/translation';

/**
 * One-line time left before the weekly rollover, in the page context bar; empty while loading.
 */
@Component({
  selector: 'app-week-countdown',
  imports: [LucideHourglass],
  templateUrl: './week-countdown.html',
  styleUrl: './week-countdown.scss',
  host: { class: 'flex items-center gap-2' },
})
export class WeekCountdown {
  /**
   * Time left before the rollover, `null` while the week loads.
   */
  public readonly remaining = input.required<RemainingTime | null>();

  /**
   * Translation service, to word the label and the time left.
   */
  private readonly translation = inject(Translation);

  /**
   * Translated "closes in" label.
   */
  protected readonly timeLabel = computed(() =>
    this.translation.translate('common.week.timeLabel'),
  );

  /**
   * Translated time left ("2d 14h"), empty while the week loads.
   */
  protected readonly remainingText = computed(() => {
    const remaining = this.remaining();

    return remaining
      ? this.translation.translate('common.week.timeRemaining', {
          days: remaining.days,
          hours: remaining.hours,
        })
      : '';
  });
}

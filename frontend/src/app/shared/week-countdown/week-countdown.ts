import { Component, computed, inject, input } from '@angular/core';
import { LucideHourglass } from '@lucide/angular';

import { RemainingTime } from '@core/date/date.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';

/**
 * One-line time left before the weekly rollover, in the page context bar; empty while loading.
 */
@Component({
  selector: 'app-week-countdown',
  imports: [TranslatePipe, LucideHourglass],
  templateUrl: './week-countdown.html',
  // Children trim to cap height: the mono label and Oswald figure have different metrics.
  host: { class: 'flex items-center gap-2' },
})
export class WeekCountdown {
  /**
   * Time left before the rollover, `null` while the week loads.
   */
  public readonly remaining = input.required<RemainingTime | null>();

  /**
   * Builds the one-string accessible name.
   */
  private readonly translation = inject(Translation);

  /**
   * Full "closes in 2d 14h" phrase for assistive technology.
   */
  protected readonly accessibleLabel = computed(() => {
    const remaining = this.remaining();

    if (!remaining) {
      return '';
    }

    const label = this.translation.translate('common.week.timeLabel');
    const value = this.translation.translate('common.week.timeRemaining', {
      days: remaining.days,
      hours: remaining.hours,
    });

    return `${label} ${value}`;
  });
}

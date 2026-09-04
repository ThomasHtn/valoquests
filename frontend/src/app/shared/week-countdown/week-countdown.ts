import { Component, computed, inject, input } from '@angular/core';
import { LucideHourglass } from '@lucide/angular';

import { RemainingTime } from '@core/date/week-period.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';

/**
 * Time left before the weekly rollover, shown at the trailing edge of the page context bar.
 *
 * The overview, quest and squad pages all count down to the same deadline and used to each carry
 * their own copy of this block; kept here so the three read identically and the label, the size of
 * the value and its alignment cannot drift apart again.
 *
 * A single line rather than the label stacked over the value it used to be: it now rides in a 56px
 * bar beside the page's title, where a two-line block would set the bar's height on its own. The
 * hourglass carries the meaning where the label does not fit — below `md` the bar has to hold a
 * burger, a title and this at once — so the value is never left as a bare figure.
 *
 * Renders nothing until {@link remaining} is known: a header that shows the label above an empty
 * line while the week loads reads as a broken value rather than as a pending one.
 */
@Component({
  selector: 'app-week-countdown',
  imports: [TranslatePipe, LucideHourglass],
  templateUrl: './week-countdown.html',
  host: { class: 'flex items-center gap-2' },
})
export class WeekCountdown {
  /**
   * Time left before the weekly rollover, or `null` while the active week is still loading.
   */
  public readonly remaining = input.required<RemainingTime | null>();

  /**
   * i18n service used to build the accessible name, which has to be one string rather than the two
   * the template lays out.
   */
  private readonly translation = inject(Translation);

  /**
   * Full "time remaining: 2d 14h" phrase, naming the figure for assistive technology and for the
   * viewports where the visible label is dropped.
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

    return `${label} : ${value}`;
  });
}

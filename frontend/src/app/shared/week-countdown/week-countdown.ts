import { Component, input } from '@angular/core';

import { RemainingTime } from '@core/date/week-period.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';

/**
 * Time left before the weekly rollover, shown on the trailing edge of a page header.
 *
 * The overview, quest and squad pages all count down to the same deadline and used to each carry
 * their own copy of this block; kept here so the three read identically and the label, the size of
 * the value and its alignment cannot drift apart again.
 *
 * Renders nothing until {@link remaining} is known: a header that shows the label above an empty
 * line while the week loads reads as a broken value rather than as a pending one.
 */
@Component({
  selector: 'app-week-countdown',
  imports: [TranslatePipe],
  template: `
    @if (remaining(); as remaining) {
      <p class="tracking-label font-mono text-xs font-medium text-text-muted uppercase">
        {{ 'overview.header.timeLabel' | translate }}
      </p>
      <p
        class="font-display text-xl leading-none font-bold text-brand-500 tabular-nums sm:mt-1.5 sm:text-2xl"
      >
        {{
          'overview.header.timeRemaining'
            | translate: { days: remaining.days, hours: remaining.hours }
        }}
      </p>
    }
  `,
  // On a narrow viewport the header's flex row has wrapped and this block starts a line of its
  // own: right-aligning it there would float it away from the title above, and stacking the label
  // over the value would cost two lines between that title and its subtitle. So it runs inline and
  // leading below `sm`, and returns to the stacked, trailing-edge block above it.
  host: { class: 'flex items-baseline gap-2.5 sm:block sm:text-right' },
})
export class WeekCountdown {
  /**
   * Time left before the weekly rollover, or `null` while the active week is still loading.
   */
  public readonly remaining = input.required<RemainingTime | null>();
}

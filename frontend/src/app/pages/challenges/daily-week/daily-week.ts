import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Tooltip } from '@shared/tooltip/tooltip';

import { DayCell } from '../challenges.model';

/**
 * Seven-day strip under the daily challenge; a drawn day shows its tally and opens in place.
 */
@Component({
  selector: 'app-daily-week',
  imports: [TranslatePipe, Tooltip],
  templateUrl: './daily-week.html',
  styleUrl: './daily-week.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[class.daily-week--slim]': 'slim()',
  },
})
export class DailyWeek {
  /**
   * The seven days, Monday first.
   */
  public readonly days = input.required<readonly DayCell[]>();

  /**
   * Whether each day shows its initial only, where full labels would crowd.
   */
  public readonly initials = input(false);

  /**
   * Whether the strip shrinks to underlined initials, scores left to tooltips.
   */
  public readonly slim = input(false);

  /**
   * Index of the day whose challenge is on screen.
   */
  public readonly selected = model<number | null>(null);

  /**
   * Shows a drawn day's challenge; an undrawn day has none to show.
   */
  protected pick(day: DayCell): void {
    if (day.drawn) {
      this.selected.set(day.index);
    }
  }
}

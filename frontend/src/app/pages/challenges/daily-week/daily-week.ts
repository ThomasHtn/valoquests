import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Tooltip } from '@shared/tooltip/tooltip';
import { DayCell } from '../challenges.model';

/**
 * The seven days of the week in one strip, beside the day's challenge heading: each day that had
 * a challenge shows how many operators finished it, and opens it in the card below. Today is in
 * the daily's cyan; the day on screen is underlined. The days ahead have nothing to open.
 */
@Component({
  selector: 'app-daily-week',
  imports: [TranslatePipe, Tooltip],
  templateUrl: './daily-week.html',
  styleUrl: './daily-week.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DailyWeek {
  /**
   * The seven days, Monday first.
   */
  public readonly days = input.required<readonly DayCell[]>();

  /**
   * Index of the day whose challenge the card shows.
   */
  public readonly selected = model<number | null>(null);
}

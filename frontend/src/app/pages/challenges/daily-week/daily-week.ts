import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Tooltip } from '@shared/tooltip/tooltip';
import { DayCell } from '../challenges.model';

/**
 * The seven days of the week as the bottom band of the day's challenge: each day that had a
 * challenge shows how many operators finished it, and opens it in place. Today is in the daily's
 * cyan; the day on screen is tinted and underlined. The days ahead have nothing to open.
 *
 * Laid flush by its caller, which knows the padding to cancel.
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
   * Whether each day is labelled by its initial alone, where seven full labels would crowd.
   */
  public readonly initials = input(false);

  /**
   * Index of the day whose challenge is on screen.
   */
  public readonly selected = model<number | null>(null);
}

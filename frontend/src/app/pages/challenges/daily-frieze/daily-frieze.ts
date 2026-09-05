import { ChangeDetectionStrategy, Component, computed, input, linkedSignal } from '@angular/core';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { ChallengeCardView } from '../challenge-card/challenge-card';
import { DayCell } from '../challenges.model';

/**
 * The seven days of the week in a line, and the drawer under them.
 *
 * Today is open at rest; a click on a past day slides its challenge into the drawer instead. The
 * days ahead have nothing to open: their challenge is drawn on the morning itself.
 */
@Component({
  selector: 'app-daily-frieze',
  imports: [TranslatePipe, ChallengeCardView],
  templateUrl: './daily-frieze.html',
  styleUrl: './daily-frieze.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DailyFrieze {
  public readonly days = input.required<readonly DayCell[]>();

  /**
   * Index of the open day. Follows today until the reader picks another day, and again once the
   * week rolls.
   */
  protected readonly pickedIndex = linkedSignal(
    () => this.days().find((day) => day.state === 'now')?.index ?? null,
  );

  protected readonly picked = computed<DayCell | null>(() => {
    const index = this.pickedIndex();
    const day = this.days().find((cell) => cell.index === index) ?? null;
    return day?.card ? day : null;
  });

  /**
   * Share of the strip's width the pointer sits at, under the open day.
   */
  protected readonly pointerX = computed(() => {
    const index = this.pickedIndex();
    const count = this.days().length;
    return index === null || count === 0 ? '50%' : `${((index + 0.5) / count) * 100}%`;
  });

  protected pick(day: DayCell): void {
    if (day.card) {
      this.pickedIndex.set(day.index);
    }
  }
}

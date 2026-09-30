import { ChangeDetectionStrategy, Component, computed, effect, input, signal } from '@angular/core';
import { LucideClock } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Tooltip } from '@shared/tooltip/tooltip';

/**
 * The day's countdown on its challenge's key line: a small clock and the time left, `08:14:52`.
 */
@Component({
  selector: 'app-daily-clock',
  imports: [TranslatePipe, Tooltip, LucideClock],
  templateUrl: './daily-clock.html',
  styleUrl: './daily-clock.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DailyClock {
  /**
   * When the day closes, in epoch milliseconds.
   */
  public readonly deadline = input.required<number>();

  private readonly secondsLeft = signal(0);

  /**
   * Time left as hours, minutes and seconds, each on two digits.
   */
  protected readonly label = computed(() => {
    const left = this.secondsLeft();
    return [Math.floor(left / 3_600), Math.floor(left / 60) % 60, left % 60]
      .map((part) => String(part).padStart(2, '0'))
      .join(':');
  });

  constructor() {
    // A new deadline restarts the clock rather than racing the old one.
    effect((onCleanup) => {
      const deadline = this.deadline();
      const beat = (): void => {
        this.secondsLeft.set(Math.max(0, Math.floor((deadline - Date.now()) / 1_000)));
      };
      beat();
      const timer = setInterval(beat, 1_000);
      onCleanup(() => clearInterval(timer));
    });
  }
}

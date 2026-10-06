import { ChangeDetectionStrategy, Component, computed, effect, input, signal } from '@angular/core';

import { TranslatePipe } from '@core/i18n/translate-pipe';

import { countdownUnits } from './countdown.utils';

/**
 * Deadline counted down every second; reduced motion only stops the beating diamond.
 */
@Component({
  selector: 'app-countdown',
  imports: [TranslatePipe],
  templateUrl: './countdown.html',
  styleUrl: './countdown.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.countdown--small]': 'size() === "sm"' },
})
export class Countdown {
  /**
   * End instant, in epoch milliseconds.
   */
  public readonly deadline = input.required<number>();

  /**
   * `md` for the extraction, `sm` for the daily challenge.
   */
  public readonly size = input<'md' | 'sm'>('md');

  /**
   * Whether to show a days slot; without it the hours absorb the days.
   */
  public readonly withDays = input(true);

  /**
   * Accessible name read in place of the figures.
   */
  public readonly label = input('');

  /**
   * Seconds left, refreshed every second.
   */
  private readonly secondsLeft = signal(0);

  /**
   * Slots on screen, days first when {@link withDays}.
   */
  protected readonly units = computed(() => countdownUnits(this.secondsLeft(), this.withDays()));

  /**
   * Restarts the one-second clock whenever the deadline changes.
   */
  constructor() {
    // Effect: the input is unreadable before binding, and a new deadline restarts the clock.
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

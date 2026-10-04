import { ChangeDetectionStrategy, Component, computed, effect, input, signal } from '@angular/core';

/**
 * Deadline counted down every second; reduced motion only stops the beating diamond.
 */
@Component({
  selector: 'app-countdown',
  templateUrl: './countdown.html',
  styleUrl: './countdown.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.is-small]': 'size() === "sm"' },
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
   * Days, then zero-padded hours, minutes and seconds, as the slots show them.
   */
  protected readonly parts = computed(() => {
    const left = this.secondsLeft();
    const days = Math.floor(left / 86_400);
    const hours = this.withDays() ? Math.floor(left / 3_600) % 24 : Math.floor(left / 3_600);

    return {
      days,
      hours: String(hours).padStart(2, '0'),
      minutes: String(Math.floor(left / 60) % 60).padStart(2, '0'),
      seconds: String(left % 60).padStart(2, '0'),
    };
  });

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

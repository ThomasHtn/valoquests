import { ChangeDetectionStrategy, Component, computed, effect, input, signal } from '@angular/core';

/**
 * A deadline counted down second by second: days when there are any, hours, minutes, seconds.
 *
 * A deadline written as a date is a date; the same one going down every second is the end of a
 * mission, which is what the page wants a reader to feel. Reduced motion keeps the countdown — it
 * is content, not decoration — and only loses the beating diamond, which lives in the stylesheet.
 */
@Component({
  selector: 'app-countdown',
  templateUrl: './countdown.html',
  styleUrl: './countdown.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.is-small]': 'size() === "sm"' },
})
export class Countdown {
  /**
   * Instant the countdown ends at, in epoch milliseconds.
   */
  public readonly deadline = input.required<number>();

  /**
   * `md` for the extraction, `sm` for the daily challenge: two deadlines, one vocabulary.
   */
  public readonly size = input<'md' | 'sm'>('md');

  /**
   * Whether to show a days slot. Without one the hours absorb the days rather than losing them.
   */
  public readonly withDays = input(true);

  /**
   * Accessible name of the whole countdown, read in place of the four figures.
   */
  public readonly label = input('');

  /**
   * Seconds left, refreshed every second.
   */
  private readonly secondsLeft = signal(0);

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
    // An effect rather than a plain interval: the deadline is a required input, unreadable until
    // the first binding, and a new deadline restarts the clock rather than racing the old one.
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

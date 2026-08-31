import { Directive, effect, ElementRef, inject, input } from '@angular/core';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { Translation } from '@core/i18n/translation';

/**
 * How long a figure takes to reach its value, in milliseconds.
 *
 * Long enough to be read as a climb rather than a flicker, short enough that nobody waits on it. The
 * figure is legible throughout — this is not a loading state.
 */
const DURATION_MS = 900;

/**
 * Counts a figure up to its value instead of printing it outright.
 *
 * For the one loud number a page exists to report — the colony's population — and nothing else. Used
 * on every figure it would turn a dashboard into a slot machine; used on the one the whole screen is
 * about, it is what makes a population read as something that grew rather than as a stored total.
 *
 * Counts from the previous value on a refresh, not from zero: once the page is open, the interesting
 * quantity is the change. Only the first render starts from nothing.
 *
 * Honours `prefers-reduced-motion` by writing the value straight out, and — importantly — writes the
 * exact target as its final act either way, so the figure on screen is never an interpolation left
 * behind by a dropped frame.
 */
@Directive({
  selector: '[appCountUp]',
})
export class CountUp {
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly translation = inject(Translation);

  /**
   * The figure to reach.
   */
  public readonly appCountUp = input.required<number>();

  /**
   * Value the current run started from — the previous target, or zero on the first render.
   *
   * A plain field, deliberately not a signal. As one it was read *and* written inside the effect
   * below, so the effect depended on it, re-entered the moment it was set, and its second pass
   * cancelled the animation its first pass had just started — the figure jumped straight to its
   * value and never climbed. Nothing renders this, so it has no business being reactive.
   */
  private previous = 0;

  /**
   * Handle of the run in flight, so a value arriving mid-climb cancels it rather than racing it.
   */
  private frame: number | null = null;

  /**
   * Starts a new climb whenever the target changes.
   */
  constructor() {
    effect(() => {
      const target = this.appCountUp();
      // Read so the figure is re-grouped when the dictionary is swapped on a language switch.
      this.translation.language();
      this.run(this.previous, target);
      this.previous = target;
    });
  }

  /**
   * Animates the host's text from one figure to another.
   *
   * @param from - Figure to start at.
   * @param to - Figure to reach.
   */
  private run(from: number, to: number): void {
    if (this.frame !== null) {
      cancelAnimationFrame(this.frame);
      this.frame = null;
    }

    if (from === to || matchMedia('(prefers-reduced-motion: reduce)').matches) {
      this.write(to);
      return;
    }

    const start = performance.now();
    const step = (now: number): void => {
      const progress = Math.min(1, (now - start) / DURATION_MS);
      // Ease-out quartic, the curve the rest of the interface decelerates on.
      const eased = 1 - Math.pow(1 - progress, 4);
      this.write(Math.round(from + (to - from) * eased));

      if (progress < 1) {
        this.frame = requestAnimationFrame(step);
        return;
      }

      this.frame = null;
      // The exact target, never the last interpolation: a figure the page reports must not be off
      // by one because a frame landed early.
      this.write(to);
    };

    this.frame = requestAnimationFrame(step);
  }

  /**
   * Writes a figure to the host, grouped the way every other amount in the interface is.
   *
   * @param value - The figure to write.
   */
  private write(value: number): void {
    this.host.nativeElement.textContent = formatDamage(value, this.translation.language());
  }
}

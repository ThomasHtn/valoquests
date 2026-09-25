import { DestroyRef, Directive, effect, ElementRef, inject, input } from '@angular/core';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { Translation } from '@core/i18n/translation';
import { DURATION_MS, VISIBILITY_THRESHOLD } from './count-up.constants';

/**
 * Counts a figure up to its value instead of printing it: from zero the first time it scrolls into
 * view, then from its previous value whenever it changes.
 *
 * Writes the text itself, so the host must be empty, with the application's grouping
 * (`formatDamage`).
 */
@Directive({
  selector: '[appCountUp]',
})
export class CountUp {
  private readonly host = inject(ElementRef<HTMLElement>);
  private readonly translation = inject(Translation);

  /**
   * Value the figure climbs to.
   */
  public readonly appCountUp = input.required<number>();

  /**
   * Value currently on screen, where the next climb starts from.
   */
  private shown = 0;

  /**
   * Whether the host has been seen yet; the first climb waits for it.
   */
  private seen = false;

  private frame: number | null = null;

  constructor() {
    const observer =
      typeof IntersectionObserver === 'undefined'
        ? null
        : new IntersectionObserver(
            (entries) => {
              if (entries.some((entry) => entry.isIntersecting)) {
                observer?.disconnect();
                this.seen = true;
                this.run(this.shown, this.appCountUp());
              }
            },
            { threshold: VISIBILITY_THRESHOLD },
          );

    if (observer) {
      observer.observe(this.host.nativeElement);
    } else {
      this.seen = true;
    }

    effect(() => {
      const target = this.appCountUp();
      // Read so the figure is re-grouped when the dictionary is swapped on a language switch.
      this.translation.language();
      if (this.seen) {
        this.run(this.shown, target);
      } else {
        this.write(this.shown);
      }
    });

    inject(DestroyRef).onDestroy(() => {
      observer?.disconnect();
      if (this.frame !== null) {
        cancelAnimationFrame(this.frame);
      }
    });
  }

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
      // Ease-out quintic: the figure races through the bulk and settles on its last digits.
      const eased = 1 - Math.pow(1 - progress, 5);
      this.write(from + (to - from) * eased);

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

  private write(value: number): void {
    this.shown = value;
    this.host.nativeElement.textContent = formatDamage(
      Math.round(value),
      this.translation.language(),
    );
  }
}

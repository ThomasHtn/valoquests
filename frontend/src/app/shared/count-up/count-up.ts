import { DestroyRef, Directive, effect, ElementRef, inject, input } from '@angular/core';

import { formatFigure } from '@core/i18n/format/number-format.utils';
import { Translation } from '@core/i18n/translation';

import { COUNT_UP_DURATION_MS, COUNT_UP_VISIBILITY_THRESHOLD } from './count-up.constants';
import { easeInOutQuad } from './count-up.utils';

/**
 * Counts a figure up to its value: from zero when first seen, then from its previous value.
 * Writes the text itself (grouped by `formatFigure`), so the host must be empty.
 */
@Directive({
  selector: '[appCountUp]',
})
export class CountUp {
  /**
   * Host element, watched for visibility and given the figure as text.
   */
  private readonly host = inject(ElementRef<HTMLElement>);

  /**
   * Translation service, whose language groups the figure's digits.
   */
  private readonly translation = inject(Translation);

  /**
   * Value the figure climbs to.
   */
  public readonly appCountUp = input.required<number>();

  /**
   * Value on screen, where the next climb starts.
   */
  private shown = 0;

  /**
   * Whether the host has been seen; the first climb waits for it.
   */
  private seen = false;

  /**
   * Pending animation frame, cancelled when a new climb starts or the host is destroyed.
   */
  private frame: number | null = null;

  /**
   * Waits for the host to scroll into view, then climbs on every new value or language.
   */
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
            { threshold: COUNT_UP_VISIBILITY_THRESHOLD },
          );

    if (observer) {
      observer.observe(this.host.nativeElement);
    } else {
      this.seen = true;
    }

    effect(() => {
      const target = this.appCountUp();
      // Tracked so the figure is re-grouped on a language switch.
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

  /**
   * Animates the figure between two values, jumping straight there under reduced motion.
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
      const progress = Math.min(1, (now - start) / COUNT_UP_DURATION_MS);
      this.write(from + (to - from) * easeInOutQuad(progress));

      if (progress < 1) {
        this.frame = requestAnimationFrame(step);
        return;
      }

      this.frame = null;
      // Exact target, so an early frame never leaves the figure off by one.
      this.write(to);
    };

    this.frame = requestAnimationFrame(step);
  }

  /**
   * Prints a rounded, grouped value and remembers it as the next climb's start.
   */
  private write(value: number): void {
    this.shown = value;
    this.host.nativeElement.textContent = formatFigure(
      Math.round(value),
      this.translation.language(),
    );
  }
}

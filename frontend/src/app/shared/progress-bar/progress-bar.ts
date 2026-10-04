import { Component, computed, input } from '@angular/core';

import { InView } from '@shared/in-view/in-view';

/**
 * Thin progress track whose width the caller sets with a `class`.
 * `aria-hidden`: every call site prints the value beside it; a caller without one must expose it.
 */
@Component({
  selector: 'app-progress-bar',
  templateUrl: './progress-bar.html',
  // The fill runs out only once the track is on screen.
  hostDirectives: [InView],
  host: {
    class: 'relative block overflow-hidden bg-surface-sunken',
    '[class]': 'heightClass() + " " + radiusClass()',
    'aria-hidden': 'true',
  },
})
export class ProgressBar {
  /**
   * Fill percentage, 0 to 100.
   */
  public readonly percentage = input.required<number>();

  /**
   * Tailwind background utility of the fill.
   */
  public readonly colorClass = input.required<string>();

  /**
   * Level a second band reaches from the fill's head (food surplus), `null` for one band.
   */
  public readonly secondaryPercentage = input<number | null>(null);

  /**
   * Tailwind background utility of the second band.
   */
  public readonly secondaryColorClass = input('');

  /**
   * Tailwind height utility of the track.
   */
  public readonly heightClass = input('h-1');

  /**
   * Tailwind radius utility of the track (square by default); the host clips the fill to it.
   */
  public readonly radiusClass = input('');

  /**
   * Draws a bright hairline at the fill's head, for bars read as gauges.
   */
  public readonly edgeMarker = input(false);

  /**
   * Static tick at the level the fill is heading for, `null` for none.
   */
  public readonly targetMarker = input<number | null>(null);

  /**
   * Runs a faint sheen along the fill; only for values still moving, never settled figures.
   */
  public readonly live = input(false);

  /**
   * Second band's width, `null` when none; clamped at 0 so it never runs backwards.
   */
  protected readonly secondaryWidth = computed<number | null>(() => {
    const secondary = this.secondaryPercentage();

    return secondary === null ? null : Math.max(0, secondary - this.percentage());
  });

  /**
   * Target tick, `null` at 0 % and 100 % where it would mark nothing.
   */
  protected readonly visibleTargetMarker = computed<number | null>(() => {
    const target = this.targetMarker();

    return target !== null && target > 0 && target < 100 ? target : null;
  });
}

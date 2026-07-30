import { Component, computed, input } from '@angular/core';

/**
 * Circumference, in SVG user units, of the ring traced by {@link ProgressCircle}'s `viewBox="0 0
 * 36 36"` circles of radius 15. Precomputed once since it is identical for every instance.
 */
const RADIUS = 15;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

/**
 * Ring-shaped progress indicator, filling clockwise from the top.
 *
 * The desktop ranking table's compact counterpart to {@link ProgressBar}: a table cell is tall
 * but narrow, so a ring reads better there than a horizontal track. Callers size it with a plain
 * `class` attribute (e.g. `class="h-8 w-8"`), since the SVG scales to fill its host.
 *
 * Hidden from assistive technology for the same reason as {@link ProgressBar}: every call site
 * renders the same value as adjacent text, so the ring is a redundant visual encoding.
 */
@Component({
  selector: 'app-progress-circle',
  templateUrl: './progress-circle.html',
  host: {
    class: 'block',
    'aria-hidden': 'true',
  },
})
export class ProgressCircle {
  /**
   * Fill percentage, from 0 to 100.
   */
  public readonly percentage = input.required<number>();

  /**
   * Tailwind text color utility applied to the filled arc, resolved by the caller so this
   * component stays agnostic of the domain the percentage comes from. Reused as the stroke color
   * through `stroke="currentColor"`, since the challenge color palette is only ever expressed as
   * `text-*` and `bg-*` utilities, never `stroke-*`.
   */
  public readonly colorClass = input.required<string>();

  /**
   * Radius, in user units, of both the track and the filled arc.
   */
  protected readonly radius = RADIUS;

  /**
   * Length, in user units, of the full ring. Passed to `stroke-dasharray` so the arc can be
   * clipped to {@link percentage} of it.
   */
  protected readonly circumference = CIRCUMFERENCE;

  /**
   * `stroke-dashoffset` hiding the uncompleted portion of the ring, computed so that 0% draws
   * nothing and 100% draws the full circle.
   */
  protected readonly dashOffset = computed(() => CIRCUMFERENCE * (1 - this.percentage() / 100));
}

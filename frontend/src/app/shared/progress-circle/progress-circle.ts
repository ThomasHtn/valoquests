import { Component, input } from '@angular/core';
import { RoundProgressComponent } from 'angular-svg-round-progressbar';

/**
 * Ring geometry, in the SVG viewBox's own units. `responsive` scales those units to whatever size
 * the caller gave the host, so the stroke stays a constant share of the diameter — 4 in 44, the
 * share `progress-ring-core` clears when it draws a disc inside the ring.
 */
const RING = {
  radius: 22,
  stroke: 4,

  /** The unfilled part of the track, on the app's own hairline token. */
  track: 'var(--color-edge)',

  /**
   * The library animates every change in `current` and offers no switch to skip it; its easing
   * divides by the duration, so zero is out and one millisecond resolves on the first frame
   * instead. The ranking matrix draws thirty-five of these at once, and none of them moved before.
   */
  durationMs: 1,
} as const;

/**
 * Ring-shaped progress indicator, filling clockwise from the top.
 *
 * The desktop ranking table's compact counterpart to {@link ProgressBar}: a table cell is tall
 * but narrow, so a ring reads better there than a horizontal track. Callers size it with a plain
 * `class` attribute (e.g. `class="h-8 w-8"`), since the ring fills its host.
 *
 * A thin adapter over `angular-svg-round-progressbar` rather than a ring drawn here: this holds the
 * one place the geometry, the track color and the arc's own color are decided, and keeps the
 * library's numeric, string-typed inputs from spreading to call sites that speak in percentages and
 * Tailwind classes.
 *
 * Hidden from assistive technology for the same reason as {@link ProgressBar}: every call site
 * renders the same value as adjacent text, so the ring is a redundant visual encoding — and the
 * host's `aria-hidden` also covers the `role="progressbar"` the library sets on its own element.
 */
@Component({
  selector: 'app-progress-circle',
  imports: [RoundProgressComponent],
  template: `
    <round-progress
      color="currentColor"
      [current]="percentage()"
      [max]="100"
      [radius]="ring.radius"
      [stroke]="ring.stroke"
      [background]="ring.track"
      [duration]="ring.durationMs"
      [responsive]="true"
      [rounded]="true"
    />
  `,
  host: {
    class: 'block',
    'aria-hidden': 'true',
    '[class]': 'colorClass()',
  },
})
export class ProgressCircle {
  /**
   * Fill percentage, from 0 to 100.
   */
  public readonly percentage = input.required<number>();

  /**
   * Tailwind text color utility applied to the filled arc, resolved by the caller so this
   * component stays agnostic of the domain the percentage comes from. Read as `currentColor` by
   * the arc, since the challenge color palette is only ever expressed as `text-*` and `bg-*`
   * utilities, never `stroke-*`.
   */
  public readonly colorClass = input.required<string>();

  protected readonly ring = RING;
}

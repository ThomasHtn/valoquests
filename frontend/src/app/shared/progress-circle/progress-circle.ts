import { Component, input } from '@angular/core';

/**
 * Ring-shaped progress indicator, filling clockwise from the top.
 *
 * The desktop ranking table's compact counterpart to {@link ProgressBar}: a table cell is tall
 * but narrow, so a ring reads better there than a horizontal track. Callers size it with a plain
 * `class` attribute (e.g. `class="h-8 w-8"`), since the ring fills its host.
 *
 * Drawn by the `progress-ring` utility — a conic gradient masked into a ring — rather than by an
 * SVG with a track circle and a dashed arc. The ranking matrix renders thirty-five of these at
 * once, and the SVG form cost three nodes each: a hundred of the page's DOM went into a decoration
 * that repeats a number already written in its middle. The one visible difference is the end of
 * the arc, which is cut square instead of rounded.
 *
 * Hidden from assistive technology for the same reason as {@link ProgressBar}: every call site
 * renders the same value as adjacent text, so the ring is a redundant visual encoding.
 */
@Component({
  selector: 'app-progress-circle',
  template: '',
  host: {
    class: 'progress-ring block',
    'aria-hidden': 'true',
    '[class]': 'colorClass()',
    '[style.--progress]': 'percentage()',
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
   * the gradient, since the challenge color palette is only ever expressed as `text-*` and `bg-*`
   * utilities, never `stroke-*`.
   */
  public readonly colorClass = input.required<string>();
}

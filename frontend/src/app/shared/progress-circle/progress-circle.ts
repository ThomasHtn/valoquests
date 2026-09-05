import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * Ring geometry, in the SVG viewBox's own units. The viewBox scales to whatever size the caller
 * gave the host, so the stroke stays a constant share of the diameter — 4 in 44, the share
 * `progress-ring-core` clears when it draws a disc inside the ring.
 */
const SIZE = 44;
const STROKE = 4;
const RADIUS = (SIZE - STROKE) / 2;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

/**
 * Ring-shaped progress indicator, filling clockwise from the top.
 *
 * The ranking table's compact counterpart to {@link ProgressBar}: a table cell is tall but narrow,
 * so a ring reads better there than a horizontal track. Callers size it with a plain `class`
 * attribute (e.g. `class="h-8 w-8"`), since the ring fills its host.
 *
 * Two circles and a dash offset, drawn here rather than through a library: the ranking matrix
 * draws dozens at once, and the library it replaced animated every value on its own timer, which
 * had to be defeated call by call. This is the one place the geometry, the track colour and the
 * arc's own colour are decided.
 *
 * Hidden from assistive technology for the same reason as {@link ProgressBar}: every call site
 * renders the same value as adjacent text, so the ring is a redundant visual encoding.
 */
@Component({
  selector: 'app-progress-circle',
  template: `
    <svg class="block size-full -rotate-90" [attr.viewBox]="viewBox" aria-hidden="true">
      <circle
        [attr.cx]="centre"
        [attr.cy]="centre"
        [attr.r]="radius"
        [attr.stroke-width]="stroke"
        fill="none"
        stroke="var(--color-edge)"
      />
      <circle
        [attr.cx]="centre"
        [attr.cy]="centre"
        [attr.r]="radius"
        [attr.stroke-width]="stroke"
        [attr.stroke-dasharray]="circumference"
        [attr.stroke-dashoffset]="dashOffset()"
        fill="none"
        stroke="currentColor"
        stroke-linecap="round"
      />
    </svg>
  `,
  host: {
    class: 'block',
    'aria-hidden': 'true',
    '[class]': 'colorClass()',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
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

  protected readonly viewBox = `0 0 ${SIZE} ${SIZE}`;

  protected readonly centre = SIZE / 2;

  protected readonly radius = RADIUS;

  protected readonly stroke = STROKE;

  protected readonly circumference = CIRCUMFERENCE;

  /**
   * Length of the arc left undrawn. Clamped so a value past the range stays a closed ring.
   */
  protected readonly dashOffset = computed(() => {
    const share = Math.min(100, Math.max(0, this.percentage())) / 100;
    return CIRCUMFERENCE * (1 - share);
  });
}

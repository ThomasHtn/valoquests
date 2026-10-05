import { Component, computed, input } from '@angular/core';

import { InView } from '@shared/in-view/in-view';

/**
 * Thin progress track whose width and height the caller sets with a `class`.
 * `aria-hidden`: every call site prints the value beside it; a caller without one must expose it.
 */
@Component({
  selector: 'app-progress-bar',
  templateUrl: './progress-bar.html',
  styleUrl: './progress-bar.scss',
  // The fill runs out only once the track is on screen.
  hostDirectives: [InView],
  host: { 'aria-hidden': 'true' },
})
export class ProgressBar {
  /**
   * Fill percentage, 0 to 100.
   */
  public readonly percentage = input.required<number>();

  /**
   * CSS colour of the fill (`var(--color-accent-green)`).
   */
  public readonly tone = input.required<string>();

  /**
   * Draws a bright hairline at the fill's head, for bars read as gauges.
   */
  public readonly edgeMarker = input(false);

  /**
   * Whether the leading-edge marker shows; skipped at 0 % and 100 % where it marks nothing.
   */
  protected readonly showsEdgeMarker = computed(() => {
    const percentage = this.percentage();

    return this.edgeMarker() && percentage > 0 && percentage < 100;
  });
}

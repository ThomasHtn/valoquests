import { Component, computed, input } from '@angular/core';

import { ChartTooltipAnchor } from './chart.model';
import { resolveChartTooltipPlacement } from './chart-tooltip.utils';

/**
 * HTML tooltip for a Chart.js canvas, placed beside the hovered mark.
 *
 * The parent projects the content and feeds the anchor from {@link trackChartTooltip}; the host
 * must sit inside the same positioned box as the canvas.
 */
@Component({
  selector: 'app-chart-tooltip',
  templateUrl: './chart-tooltip.html',
  host: { class: 'contents' },
})
export class ChartTooltip {
  /**
   * The hovered mark, or `null` to hide the bubble.
   */
  public readonly anchor = input<ChartTooltipAnchor | null>(null);

  /**
   * Where the bubble goes, or `null` while hidden.
   */
  protected readonly placement = computed(() => {
    const anchor = this.anchor();
    return anchor ? resolveChartTooltipPlacement(anchor) : null;
  });
}

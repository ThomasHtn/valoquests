import { Component, computed, input } from '@angular/core';

import { ChartTooltipAnchor } from '../chart.model';
import { resolveChartTooltipPlacement } from './chart-tooltip.utils';

/**
 * HTML tooltip fed by `trackChartTooltip`; the host shares the canvas' positioned box.
 */
@Component({
  selector: 'app-chart-tooltip',
  templateUrl: './chart-tooltip.html',
  styleUrl: './chart-tooltip.scss',
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

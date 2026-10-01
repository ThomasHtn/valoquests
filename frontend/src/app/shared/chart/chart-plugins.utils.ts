import { ChartType, Plugin } from 'chart.js';

import { ChartTheme } from './chart-theme.model';

/**
 * Builds the vertical rule following the pointer.
 *
 * Chart.js has no crosshair of its own, and an indexed tooltip without one leaves the reader
 * guessing which abscissa the figures belong to on a chart hundreds of points wide.
 *
 * @param theme - Resolved chart palette.
 * @returns The crosshair plugin, scoped to one chart instance.
 */
export function createCrosshairPlugin<T extends ChartType>(theme: ChartTheme): Plugin<T> {
  return {
    id: 'crosshair',
    afterDatasetsDraw(chart) {
      const active = chart.tooltip?.getActiveElements() ?? [];
      if (active.length === 0) {
        return;
      }

      const { ctx, chartArea } = chart;
      ctx.save();
      ctx.beginPath();
      ctx.lineWidth = 1;
      ctx.strokeStyle = theme.grid;
      ctx.moveTo(active[0].element.x, chartArea.top);
      ctx.lineTo(active[0].element.x, chartArea.bottom);
      ctx.stroke();
      ctx.restore();
    },
  };
}

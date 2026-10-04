import { ChartType, Plugin } from 'chart.js';

import { ChartTheme } from './chart-theme.model';

/**
 * Vertical rule following the pointer, which Chart.js lacks.
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

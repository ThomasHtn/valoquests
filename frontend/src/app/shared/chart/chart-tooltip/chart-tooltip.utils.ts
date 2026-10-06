import { Chart, ChartType, TooltipModel } from 'chart.js';

import { ChartTooltipAnchor, ChartTooltipPlacement } from '../chart.model';
import {
  CHART_TOOLTIP_COMPACT_WIDTH,
  CHART_TOOLTIP_GAP,
  CHART_TOOLTIP_HALF_HEIGHT,
  CHART_TOOLTIP_HALF_WIDTH,
  CHART_TOOLTIP_SIDE_ROOM,
} from './chart-tooltip.constants';

/**
 * `plugins.tooltip.external` handler reporting the hovered mark (`null` on leave) for HTML.
 */
export function trackChartTooltip<T extends ChartType>(
  onChange: (anchor: ChartTooltipAnchor | null) => void,
): (context: { chart: Chart; tooltip: TooltipModel<T> }) => void {
  return ({ chart, tooltip }) => {
    const point = tooltip.dataPoints?.[0];
    if (tooltip.opacity === 0 || !point) {
      onChange(null);
      return;
    }
    onChange({
      index: point.dataIndex,
      x: tooltip.caretX,
      y: tooltip.caretY,
      chartWidth: chart.width,
      chartHeight: chart.height,
    });
  };
}

/**
 * Beside the mark on a wide chart, centered above or below it on a narrow one.
 */
export function resolveChartTooltipPlacement(anchor: ChartTooltipAnchor): ChartTooltipPlacement {
  const gap = CHART_TOOLTIP_GAP;

  if (anchor.chartWidth < CHART_TOOLTIP_COMPACT_WIDTH) {
    const left = clamp(
      anchor.x,
      CHART_TOOLTIP_HALF_WIDTH,
      anchor.chartWidth - CHART_TOOLTIP_HALF_WIDTH,
    );
    const below = anchor.y < anchor.chartHeight / 2;
    return {
      left,
      top: anchor.y,
      transform: below ? `translate(-50%, ${gap}px)` : `translate(-50%, calc(-100% - ${gap}px))`,
    };
  }

  const onRight = anchor.x < anchor.chartWidth - CHART_TOOLTIP_SIDE_ROOM;
  return {
    left: anchor.x,
    top: clamp(anchor.y, CHART_TOOLTIP_HALF_HEIGHT, anchor.chartHeight - CHART_TOOLTIP_HALF_HEIGHT),
    transform: onRight ? `translate(${gap}px, -50%)` : `translate(calc(-100% - ${gap}px), -50%)`,
  };
}

/**
 * Clamps a value, favoring `min` when the range is empty.
 */
function clamp(value: number, min: number, max: number): number {
  return Math.max(min, Math.min(value, Math.max(min, max)));
}

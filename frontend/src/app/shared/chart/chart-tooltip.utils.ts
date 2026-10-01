import { Chart, ChartType, TooltipModel } from 'chart.js';

import {
  CHART_TOOLTIP_COMPACT_WIDTH,
  CHART_TOOLTIP_GAP,
  CHART_TOOLTIP_HALF_HEIGHT,
  CHART_TOOLTIP_HALF_WIDTH,
  CHART_TOOLTIP_SIDE_ROOM,
} from './chart-tooltip.constants';
import { ChartTooltipAnchor, ChartTooltipPlacement } from './chart.model';

/**
 * Builds a Chart.js `external` tooltip handler that reports the hovered mark instead of drawing.
 *
 * Chart.js' own bubble only sets text on the canvas; handing the position out lets a component
 * render the tooltip in HTML, with icons, chips and translated labels.
 *
 * @param onChange - Receives the hovered mark, or `null` once the pointer leaves the plot.
 * @returns The handler to plug into `plugins.tooltip.external`.
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
 * Places an HTML tooltip next to its mark.
 *
 * Beside the mark on a wide chart, on whichever side has room; centred above it on a narrow one,
 * or below when the mark sits in the upper half and the bubble would leave the plot.
 *
 * @param anchor - The hovered mark.
 * @returns The bubble's anchor point and the transform moving it off the mark.
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
 * Keeps a value inside a range, favouring the lower bound when the range is empty.
 *
 * @param value - The value to bound.
 * @param min - Lower bound.
 * @param max - Upper bound.
 * @returns The bounded value.
 */
function clamp(value: number, min: number, max: number): number {
  return Math.max(min, Math.min(value, Math.max(min, max)));
}

import { describe, expect, it } from 'vitest';

import { resolveChartTooltipPlacement } from './chart-tooltip.utils';

describe('resolveChartTooltipPlacement', () => {
  it('puts the bubble right of a mark on the left of a wide chart', () => {
    expect(
      resolveChartTooltipPlacement({ index: 0, x: 100, y: 150, chartWidth: 900, chartHeight: 288 }),
    ).toEqual({ left: 100, top: 150, transform: 'translate(16px, -50%)' });
  });

  it('flips the bubble left of a mark near the right edge', () => {
    const placement = resolveChartTooltipPlacement({
      index: 0,
      x: 800,
      y: 150,
      chartWidth: 900,
      chartHeight: 288,
    });

    expect(placement.transform).toBe('translate(calc(-100% - 16px), -50%)');
  });

  it('keeps a side bubble inside the chart vertically', () => {
    expect(
      resolveChartTooltipPlacement({ index: 0, x: 100, y: 10, chartWidth: 900, chartHeight: 288 })
        .top,
    ).toBe(90);
  });

  it('centres the bubble under a high mark on a narrow chart, inside its edges', () => {
    expect(
      resolveChartTooltipPlacement({ index: 0, x: 20, y: 40, chartWidth: 340, chartHeight: 256 }),
    ).toEqual({ left: 135, top: 40, transform: 'translate(-50%, 16px)' });
  });

  it('centres the bubble over a low mark on a narrow chart', () => {
    const placement = resolveChartTooltipPlacement({
      index: 0,
      x: 170,
      y: 200,
      chartWidth: 340,
      chartHeight: 256,
    });

    expect(placement.transform).toBe('translate(-50%, calc(-100% - 16px))');
  });
});

import { describe, expect, it } from 'vitest';

import { base, campaign, week } from '../overview.fixtures';
import { buildCapacity, hullFigureSize } from './extraction-gauges.utils';

describe('hullFigureSize', () => {
  it('shrinks the figure as it gains digits', () => {
    expect(hullFigureSize(46)).toBe('2rem');
    expect(hullFigureSize(446)).toBe('1.875rem');
    expect(hullFigureSize(1959)).toBe('1.25rem');
    expect(hullFigureSize(1_234_567)).toBe('0.875rem');
  });
});

describe('buildCapacity', () => {
  it('returns null outside a running week with a forecast', () => {
    expect(buildCapacity(campaign({ forecast: null }), week())).toBeNull();
  });

  it('caps every fraction at one wounded fully covered', () => {
    const capacity = buildCapacity(
      campaign({
        base: base({ rescuesByComponents: 20, rescuesByFood: 3 }),
        forecast: {
          weekIndex: 1,
          woundedCount: 10,
          challengeRescued: 2,
          extractionRescued: 5,
          rescued: 7,
          leftBehind: 3,
          limiter: 'FOOD',
        },
      }),
      week({ woundedCount: 10 }),
    );

    expect(capacity?.carry).toMatchObject({ value: 20, fraction: 1, stock: 100 });
    expect(capacity?.shelter).toMatchObject({ value: 3, fraction: 0.3 });
    expect(capacity?.aboardFraction).toBe(0.7);
  });
});

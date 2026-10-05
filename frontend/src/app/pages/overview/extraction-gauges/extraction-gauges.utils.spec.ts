import { describe, expect, it } from 'vitest';

import { base, campaign, translate, week } from '../overview.fixtures';
import { buildCapacity, buildLimitDials, hullFigureSize } from './extraction-gauges.utils';

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

describe('buildLimitDials', () => {
  const capacity = buildCapacity(
    campaign({
      base: base({ rescuesByComponents: 20, componentsPerRescue: 14 }),
      forecast: {
        weekIndex: 1,
        woundedCount: 10,
        challengeRescued: 0,
        extractionRescued: 0,
        rescued: 0,
        leftBehind: 10,
        limiter: 'FOOD',
      },
    }),
    week({ woundedCount: 10, progressPercent: 40, guardianHitPoints: 1000 }),
  )!;
  const dials = buildLimitDials(capacity, 'Boss 01', translate, (amount) => `#${amount}`);

  it('lists carry, shelter then breakthrough', () => {
    expect(dials.map((dial) => dial.key)).toEqual(['carry', 'shelter', 'breach']);
  });

  it('reads the carry dial over the wounded, its rate left raw', () => {
    expect(dials[0]).toMatchObject({
      value: 20,
      unit: '',
      stock: '#100',
      rate: '14',
      ariaLabel: 'overview.capacity.carryAria(#20,#10,100)',
    });
  });

  it('reads the breakthrough in percent, against all game modes', () => {
    expect(dials[2]).toMatchObject({
      value: 40,
      unit: ' %',
      rate: '#10',
      modes: ['overview.capacity.allModes'],
      ariaLabel: 'overview.capacity.breachAria(Boss 01,40)',
    });
  });
});

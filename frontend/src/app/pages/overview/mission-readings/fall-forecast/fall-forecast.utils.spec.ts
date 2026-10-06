import { describe, expect, it } from 'vitest';

import { CampaignWeek } from '@core/campaign/campaign-week.model';

import { DAY_MS, HOUR_MS } from './fall-forecast.constants';
import {
  buildGuardianFall,
  layoutFallChart,
  readFallAt,
  smoothPath,
  splitSpan,
  toNearestHour,
} from './fall-forecast.utils';

/**
 * Monday 2026-01-05, 00:00 in Paris (UTC+1 in winter).
 */
const MONDAY = Date.parse('2026-01-04T23:00:00Z');

/**
 * Wednesday noon of that week.
 */
const WEDNESDAY_NOON = MONDAY + 2.5 * DAY_MS;

function week(overrides: Partial<CampaignWeek> = {}): CampaignWeek {
  return {
    weekIndex: 1,
    weekStart: '2026-01-05',
    planetName: 'Kepler',
    category: 'STANDARD',
    guardianName: 'Vex',
    guardianDescription: null,
    guardianHitPoints: 1000,
    damageDealt: 600,
    dailyDamage: [200, 300, 100],
    progressPercent: 60,
    defeated: false,
    defeatedAt: null,
    defeatedByPlayerId: null,
    fatalBlow: null,
    woundedCount: 10,
    challengeRescued: 0,
    extractionRescued: 0,
    foodSpent: 0,
    componentsSpent: 0,
    limiter: 'NONE',
    baseLoss: 0,
    settled: false,
    base: null,
    ...overrides,
  };
}

describe('buildGuardianFall', () => {
  it('has nothing to forecast outside a week or before the first hit', () => {
    expect(buildGuardianFall(null, WEDNESDAY_NOON)).toBeNull();
    expect(
      buildGuardianFall(week({ damageDealt: 0, dailyDamage: [0] }), MONDAY + HOUR_MS),
    ).toBeNull();
  });

  it('closes each finished day, pins now on the week total and projects the fall before Sunday', () => {
    const fall = buildGuardianFall(week(), WEDNESDAY_NOON);

    expect(fall?.outcome).toBe('ahead');
    expect(fall?.readings).toEqual([
      { kind: 'start', time: MONDAY, left: 1000 },
      { kind: 'dayEnd', time: MONDAY + DAY_MS, left: 800 },
      { kind: 'dayEnd', time: MONDAY + 2 * DAY_MS, left: 500 },
      { kind: 'now', time: WEDNESDAY_NOON, left: 400 },
    ]);
    // 600 HP in two and a half days: the last 400 take another day and two thirds.
    expect(fall?.fallAt).toBeCloseTo(WEDNESDAY_NOON + (5 / 3) * DAY_MS, -3);
    expect(fall?.leftAtDeadline).toBe(0);
  });

  it('says what would still stand at Sunday midnight when the pace falls short', () => {
    const fall = buildGuardianFall(week({ guardianHitPoints: 10_000 }), WEDNESDAY_NOON);

    expect(fall?.outcome).toBe('short');
    expect(fall?.fallAt).toBeNull();
    // 240 HP a day for the four and a half days left.
    expect(fall?.leftAtDeadline).toBeCloseTo(9_400 - 240 * 4.5, 6);
  });

  it('ends the descent on the fatal blow, leaving out the days after it', () => {
    const killAt = MONDAY + DAY_MS + 21 * HOUR_MS;
    const fall = buildGuardianFall(
      week({
        damageDealt: 1000,
        dailyDamage: [400, 700, 300],
        defeated: true,
        defeatedAt: new Date(killAt).toISOString(),
      }),
      WEDNESDAY_NOON,
    );

    expect(fall?.outcome).toBe('down');
    expect(fall?.fallAt).toBe(killAt);
    expect(fall?.readings.map((reading) => reading.kind)).toEqual(['start', 'dayEnd', 'kill']);
    expect(fall?.readings.at(-1)?.left).toBe(0);
  });
});

describe('readFallAt', () => {
  it('snaps to the nearest known reading over the played part', () => {
    const fall = buildGuardianFall(week(), WEDNESDAY_NOON)!;

    expect(readFallAt(fall, MONDAY + 1.2 * DAY_MS)).toEqual({
      kind: 'dayEnd',
      time: MONDAY + DAY_MS,
      left: 800,
    });
  });

  it('projects beyond now, down to zero', () => {
    const fall = buildGuardianFall(week(), WEDNESDAY_NOON)!;

    expect(readFallAt(fall, WEDNESDAY_NOON + DAY_MS)).toEqual({
      kind: 'estimate',
      time: WEDNESDAY_NOON + DAY_MS,
      left: 160,
    });
    expect(readFallAt(fall, MONDAY + 6 * DAY_MS).kind).toBe('zero');
  });
});

describe('layoutFallChart', () => {
  it('tints the played part, the projection and the spare time across the whole width', () => {
    const chart = layoutFallChart(buildGuardianFall(week(), WEDNESDAY_NOON)!, 700);

    expect(chart.zones.map((zone) => zone.kind)).toEqual(['past', 'ahead', 'spare']);
    expect(chart.zones.reduce((sum, zone) => sum + zone.width, 0)).toBeCloseTo(700, 6);
    expect(chart.now).not.toBeNull();
    expect(chart.end).toBeNull();
    expect(chart.fall?.x).toBeCloseTo(chart.zones[2].x, 6);
  });

  it('pins the projection at Sunday midnight when it falls short', () => {
    const chart = layoutFallChart(
      buildGuardianFall(week({ guardianHitPoints: 10_000 }), WEDNESDAY_NOON)!,
      700,
    );

    expect(chart.zones.map((zone) => zone.kind)).toEqual(['past', 'short']);
    expect(chart.fall).toBeNull();
    expect(chart.end).not.toBeNull();
  });
});

describe('smoothPath', () => {
  it('draws one cubic per gap between readings, never above the higher of its two ends', () => {
    const path = smoothPath([
      { x: 0, y: 10 },
      { x: 50, y: 40 },
      { x: 100, y: 45 },
    ]);

    expect(path.startsWith('M0,10')).toBe(true);
    expect(path.match(/C/g)).toHaveLength(2);
    const controls = [...path.matchAll(/(-?[\d.]+),(-?[\d.]+)/g)].map((match) => Number(match[2]));
    expect(Math.min(...controls)).toBeGreaterThanOrEqual(10);
    expect(Math.max(...controls)).toBeLessThanOrEqual(45);
  });
});

describe('time helpers', () => {
  it('splits a duration into whole days and hours', () => {
    expect(splitSpan(DAY_MS + 4.5 * HOUR_MS)).toEqual({ days: 1, hours: 4 });
    expect(splitSpan(-HOUR_MS)).toEqual({ days: 0, hours: 0 });
  });

  it('rounds an estimate to the nearest hour', () => {
    expect(toNearestHour(MONDAY + 19.6 * HOUR_MS)).toBe(MONDAY + 20 * HOUR_MS);
  });
});

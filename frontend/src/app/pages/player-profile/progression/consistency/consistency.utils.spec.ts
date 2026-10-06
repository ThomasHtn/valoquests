import { describe, expect, it } from 'vitest';

import {
  ConsistencyMatch,
  ConsistencySummary,
} from '@core/players/progression/player-progression.model';

import {
  buildConsistencyAxis,
  buildConsistencyFigures,
  resolveConsistencyStackHeight,
  resolveConsistencyZone,
  stackConsistencyDots,
} from './consistency.utils';

/**
 * Fake translation echoing the key and its parameters.
 */
function translate(key: string, params?: Readonly<Record<string, string | number>>): string {
  const suffix = Object.entries(params ?? {})
    .map(([name, value]) => `${name}=${value}`)
    .join(',');
  return suffix ? `${key}(${suffix})` : key;
}

/**
 * Builds one plotted match.
 */
function match(acs: number): ConsistencyMatch {
  return {
    startedAt: '2026-09-01T20:00:00Z',
    acs,
    result: 'WIN',
    allyScore: 13,
    enemyScore: 9,
    mapName: 'Ascent',
    agentName: 'Jett',
  };
}

/**
 * Builds a spread over the given combat scores.
 */
function summary(
  scores: readonly number[],
  overrides: Partial<ConsistencySummary> = {},
): ConsistencySummary {
  return {
    floor: 200,
    median: 230,
    ceiling: 260,
    spread: 60,
    seasonCount: 1,
    previousSeasonName: null,
    previousSpread: null,
    matches: scores.map(match),
    ...overrides,
  };
}

describe('buildConsistencyAxis', () => {
  it('rounds the ends to fifty and starts the columns on the lowest score', () => {
    expect(buildConsistencyAxis(summary([132, 247, 401]))).toEqual({
      min: 100,
      max: 450,
      binOrigin: 120,
      binCount: 17,
    });
  });
});

describe('resolveConsistencyZone', () => {
  it('places a score against the floor and ceiling, both bounds inside', () => {
    const shown = summary([]);
    expect(resolveConsistencyZone(199, shown)).toBe('below');
    expect(resolveConsistencyZone(200, shown)).toBe('inside');
    expect(resolveConsistencyZone(260, shown)).toBe('inside');
    expect(resolveConsistencyZone(261, shown)).toBe('above');
  });
});

describe('stackConsistencyDots', () => {
  it('stacks matches of one column on top of each other', () => {
    const shown = summary([205, 150, 215]);
    const dots = stackConsistencyDots(shown, buildConsistencyAxis(shown));

    expect(dots.map((dot) => [dot.x, dot.y, dot.zone])).toEqual([
      [150, 0.5, 'below'],
      [210, 0.5, 'inside'],
      [210, 1.5, 'inside'],
    ]);
  });
});

describe('resolveConsistencyStackHeight', () => {
  it('keeps a free level above the tallest stack, on an even tick, never under six', () => {
    const shown = summary(new Array<number>(7).fill(210));
    const dots = stackConsistencyDots(shown, buildConsistencyAxis(shown));

    expect(resolveConsistencyStackHeight(dots)).toBe(8);
    expect(resolveConsistencyStackHeight([])).toBe(6);
  });

  it('switches to steps of five above ten matches', () => {
    const shown = summary(new Array<number>(17).fill(210));
    const dots = stackConsistencyDots(shown, buildConsistencyAxis(shown));

    expect(resolveConsistencyStackHeight(dots)).toBe(20);
  });
});

describe('buildConsistencyFigures', () => {
  it('reports floor, median, ceiling and the sample without a previous season', () => {
    const figures = buildConsistencyFigures(summary([200, 230, 260]), translate);

    expect(figures).toHaveLength(4);
    expect(figures[3]).toMatchObject({
      value: '3',
      detail: 'playerProfile.progression.consistency.figures.matchesDetailOne(count=1)',
    });
  });

  it('counts the acts of a multi-season selection', () => {
    expect(buildConsistencyFigures(summary([], { seasonCount: 4 }), translate)[3].detail).toBe(
      'playerProfile.progression.consistency.figures.matchesDetail(count=4)',
    );
  });

  it('calls a clearly narrower spread than the previous season more consistent', () => {
    const figures = buildConsistencyFigures(
      summary([], { spread: 61, previousSpread: 108, previousSeasonName: 'e11a4' }),
      translate,
    );

    expect(figures[4]).toMatchObject({
      value: 'playerProfile.progression.consistency.figures.tighter',
      icon: 'tighter',
      tone: 'good',
    });
  });

  it('calls a spread within ten percent as consistent as before', () => {
    const figures = buildConsistencyFigures(
      summary([], { spread: 70, previousSpread: 73, previousSeasonName: 'e11a2' }),
      translate,
    );

    expect(figures[4]).toMatchObject({ icon: 'flat', tone: 'neutral' });
  });
});

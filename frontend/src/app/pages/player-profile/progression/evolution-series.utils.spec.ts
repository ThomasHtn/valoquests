import { describe, expect, it } from 'vitest';

import { SeasonEvolution } from '@core/players/player-progression.model';
import { buildEvolutionSeries } from './evolution-series.utils';

/**
 * Builds a season whose plotted matches carry the supplied headshot rates.
 *
 * @param seasonId - Internal season identifier.
 * @param headshotRates - One headshot rate per match, oldest first.
 * @returns The season entry.
 */
function season(seasonId: number, headshotRates: readonly number[]): SeasonEvolution {
  return {
    seasonId,
    seasonName: `Episode ${seasonId}`,
    active: false,
    points: headshotRates.map((headshotPercentage) => ({
      startedAt: '2026-08-03T10:00:00Z',
      headshotPercentage,
      kda: 1.5,
      acs: 230,
      adr: 150,
    })),
    averages: { headshotPercentage: 25, kda: 1.5, acs: 230, adr: 150 },
  };
}

describe('buildEvolutionSeries', () => {
  const colorOf = (seasonId: number): string => `color-${seasonId}`;

  it('plots a single season as its raw match sequence, with no padding', () => {
    const series = buildEvolutionSeries([season(1, [20, 22, 24])], 'headshotPercentage', colorOf);

    expect(series).toHaveLength(1);
    expect(series[0].points).toEqual([20, 22, 24]);
    expect(series[0].color).toBe('color-1');
  });

  it('pads the shorter season at the front so both end on the same abscissa', () => {
    const series = buildEvolutionSeries(
      [season(1, [10, 11, 12, 13]), season(2, [20, 21])],
      'headshotPercentage',
      colorOf,
    );

    expect(series[0].points).toEqual([10, 11, 12, 13]);
    expect(series[1].points).toEqual([null, null, 20, 21]);
  });

  it('keeps every curve the same length, so the axis fits the longest season', () => {
    const series = buildEvolutionSeries(
      [season(1, [1]), season(2, [1, 2, 3]), season(3, [1, 2])],
      'headshotPercentage',
      colorOf,
    );

    expect(series.map((curve) => curve.points.length)).toEqual([3, 3, 3]);
  });

  it('plots the requested metric rather than always the first one', () => {
    const series = buildEvolutionSeries([season(1, [20, 22])], 'acs', colorOf);

    expect(series[0].points).toEqual([230, 230]);
  });

  it('returns nothing when no season is in scope', () => {
    expect(buildEvolutionSeries([], 'kda', colorOf)).toEqual([]);
  });
});

import { describe, expect, it } from 'vitest';

import { PlayerStatistics } from '@core/players/player-details.model';

import { buildStatStrip, resolveCurrentSeasonId } from './player-profile.utils';

describe('resolveCurrentSeasonId', () => {
  it('prefers the active season', () => {
    expect(
      resolveCurrentSeasonId([
        { id: 2, name: 'e9a2', active: false },
        { id: 1, name: 'e9a1', active: true },
      ]),
    ).toBe(1);
  });

  it('falls back to the first known season, then to null', () => {
    expect(resolveCurrentSeasonId([{ id: 5, name: 'e9a5', active: false }])).toBe(5);
    expect(resolveCurrentSeasonId([])).toBeNull();
  });
});

describe('buildStatStrip', () => {
  const statistics = {
    kda: 1.5,
    winRate: 50,
    adr: 140.4,
    acs: 220.6,
    headshotPercentage: 25,
    matchesPlayed: 4,
    wins: 2,
    losses: 2,
  } as PlayerStatistics;

  it('formats every figure of a played selection', () => {
    const strip = buildStatStrip(statistics, 'en');
    expect(strip.adrLabel).toBe('140');
    expect(strip.acsLabel).toBe('221');
    expect(strip.matchesPlayed).toBe(4);
  });

  it('dashes every figure without a match', () => {
    const strip = buildStatStrip({ ...statistics, matchesPlayed: 0 }, 'en');
    expect(strip.adrLabel).toBe('—');
    expect(strip.acsLabel).toBe('—');
  });
});

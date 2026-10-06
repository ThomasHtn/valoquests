import { describe, expect, it } from 'vitest';

import { Match } from '@core/matches/match.model';

import { groupMatchesByDay } from './match-day.utils';

/**
 * A match with plausible figures, overridable per test.
 */
function match(overrides: Partial<Match> = {}): Match {
  return {
    id: 1,
    startedAt: '2026-07-28T19:00:00Z',
    mapName: 'Ascent',
    gameMode: 'COMPETITIVE',
    agentName: 'Jett',
    result: 'WIN',
    allyScore: 13,
    enemyScore: 9,
    kills: 20,
    deaths: 10,
    assists: 5,
    kd: 2,
    acs: 250,
    adr: 160,
    headshotPercentage: 25,
    competitiveTier: 'GOLD_1',
    rankRating: null,
    valoquestsDamage: 1200,
    damageCoefficientPercent: 100,
    ...overrides,
  };
}

describe('groupMatchesByDay', () => {
  it('groups consecutive matches of one campaign day and sums their record', () => {
    const [day, ...rest] = groupMatchesByDay(
      [match(), match({ id: 2, result: 'LOSS', kills: 10, deaths: 20 })],
      'en',
    );

    expect(rest).toHaveLength(0);
    expect(day.wins).toBe(1);
    expect(day.losses).toBe(1);
    expect(day.totalKills).toBe(30);
    expect(day.avgKd).toBe(1);
  });

  it('averages only the matches reporting a figure', () => {
    const [day] = groupMatchesByDay(
      [match(), match({ id: 2, acs: null, adr: null, headshotPercentage: null })],
      'en',
    );

    expect(day.avgAcs).toBe(250);
    expect(day.avgAdr).toBe(160);
    expect(day.avgHeadshotPercentage).toBe(25);
  });

  it('reports no average when no match of the day has the figure', () => {
    const [day] = groupMatchesByDay([match({ acs: null })], 'en');

    expect(day.avgAcs).toBeNull();
  });

  it('starts a new day past campaign midnight', () => {
    const days = groupMatchesByDay(
      [match({ startedAt: '2026-07-28T22:30:00Z' }), match({ startedAt: '2026-07-28T21:30:00Z' })],
      'en',
    );

    expect(days.map((day) => day.dayKey)).toEqual(['2026-07-29', '2026-07-28']);
  });
});

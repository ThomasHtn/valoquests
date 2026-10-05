import { describe, expect, it } from 'vitest';

import { HistoryMatch, MatchDay } from '@core/matches/day/match-day.model';
import {
  buildMatchHistoryDays,
  reportedTone,
  resolveKdTone,
  resolveResultTone,
} from './match-history.utils';

/**
 * Translation stub echoing the key and its percent, so assertions read which sentence was picked.
 */
const translate = (key: string, params?: Readonly<Record<string, string | number>>): string =>
  params?.['percent'] === undefined ? key : `${key}:${params['percent']}`;

/**
 * A match with plausible figures, overridable per test.
 */
function match(overrides: Partial<HistoryMatch> = {}): HistoryMatch {
  return {
    id: 7,
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
    acs: 250.4,
    adr: 160.6,
    headshotPercentage: 25,
    competitiveTier: null,
    rankRating: null,
    valoquestsDamage: 1200,
    damageCoefficientPercent: 100,
    ...overrides,
  } as HistoryMatch;
}

/**
 * A day holding the given matches.
 */
function day(matches: readonly HistoryMatch[]): MatchDay<HistoryMatch> {
  return {
    dayKey: '2026-07-28',
    dateLabel: '28/07/2026',
    wins: 1,
    losses: 0,
    matches,
    avgKd: 2,
    avgHeadshotPercentage: null,
    avgAdr: 160.6,
    avgAcs: 250.4,
    totalKills: 20,
    totalDeaths: 10,
    totalAssists: 5,
    totalValoquestsDamage: 1200,
  };
}

describe('resolveKdTone', () => {
  it('marks a K/D at or above 1 as good and below as average', () => {
    expect(resolveKdTone(1)).toBe('good');
    expect(resolveKdTone(0.8)).toBe('average');
  });

  it('mutes a missing or infinite K/D, shown as a dash', () => {
    expect(resolveKdTone(null)).toBe('muted');
    expect(resolveKdTone(Infinity)).toBe('muted');
  });
});

describe('reportedTone', () => {
  it('mutes only the figures a mode did not report', () => {
    expect(reportedTone(0)).toBe('primary');
    expect(reportedTone(null)).toBe('muted');
  });
});

describe('resolveResultTone', () => {
  it('keeps draws, remakes and unknown results neutral', () => {
    expect(resolveResultTone('WIN')).toBe('win');
    expect(resolveResultTone('LOSS')).toBe('loss');
    expect(resolveResultTone('DRAW')).toBe('neutral');
    expect(resolveResultTone('REMAKE')).toBe('neutral');
  });
});

describe('buildMatchHistoryDays', () => {
  it('formats the day figures in column order, muting the unreported ones', () => {
    const [built] = buildMatchHistoryDays([day([match()])], 3, 'en', translate);

    expect(built.stats.map((stat) => [stat.column, stat.value, stat.tone])).toEqual([
      ['kda', '20/10/5', 'primary'],
      ['kd', '2.00', 'primary'],
      ['headshotPercentage', '—', 'muted'],
      ['adr', '161', 'primary'],
      ['acs', '250', 'primary'],
    ]);
  });

  it('links a squad match to its player, and a profile match to the profile owner', () => {
    const squadMatch = match({ player: { id: 9, name: 'Kenshiro', portrait: null } });
    const [built] = buildMatchHistoryDays(
      [day([squadMatch, match({ id: 8 })])],
      3,
      'en',
      translate,
    );

    expect(built.rows.map((row) => row.link)).toEqual([
      ['/players', 9, 'matches', 7],
      ['/players', 3, 'matches', 8],
    ]);
  });

  it('writes the kept share only when the ladder reduced the damage', () => {
    const reduced = match({ damageCoefficientPercent: 50, valoquestsDamage: 600 });
    const unvalued = match({ id: 8, damageCoefficientPercent: 0, valoquestsDamage: 0 });
    const [built] = buildMatchHistoryDays([day([reduced, unvalued])], 3, 'en', translate);
    const [reducedCell, unvaluedCell] = built.rows.map((row) => row.damage);

    expect(reducedCell.reducedShareLabel).toBe('playerProfile.matches.damage.share:50');
    expect(reducedCell.explanation).toBe('playerProfile.matches.damage.reduced:50');
    expect(unvaluedCell.reducedShare).toBeNull();
    expect(unvaluedCell.isZero).toBe(true);
  });
});

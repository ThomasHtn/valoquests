import { describe, expect, it } from 'vitest';

import { WeeklyTitle } from '@core/campaign/titles/campaign-title.model';
import { MatchDetail } from '@core/matches/match.model';

import { buildMatchFigures, buildMatchTeammateRows } from './match-detail.utils';

/**
 * Match with only the fields the builders read.
 */
const match = {
  kills: 12,
  deaths: 8,
  assists: 4,
  kd: 1.5,
  acs: 240.4,
  adr: null,
  damageDealt: 3200,
  roundsPlayed: 21,
  teammates: [
    {
      playerId: 2,
      displayName: 'Ally',
      portrait: null,
      agentName: 'Sage',
      sameTeam: true,
      result: 'WIN',
      kills: 5,
      deaths: 6,
      assists: 7,
    },
    {
      playerId: 3,
      displayName: 'Rival',
      portrait: null,
      agentName: 'Jett',
      sameTeam: false,
      result: 'LOSS',
      kills: 9,
      deaths: 9,
      assists: 1,
    },
  ],
} as unknown as MatchDetail;

describe('buildMatchFigures', () => {
  it('lists the row figures, then raw damage and rounds', () => {
    const figures = buildMatchFigures(match, 'en');
    expect(figures.map((figure) => figure.value)).toEqual([
      '12/8/4',
      expect.any(String),
      '240',
      '—',
      expect.any(String),
      '21',
    ]);
    expect(figures.filter((figure) => figure.hintKey === null)).toHaveLength(2);
  });
});

describe('buildMatchTeammateRows', () => {
  it('flags the champion and gives titles to the others only', () => {
    const rows = buildMatchTeammateRows(
      match,
      2,
      new Map<number, WeeklyTitle>([
        [2, 'SCOUT'],
        [3, 'SCOUT'],
      ]),
    );
    expect(rows[0]).toMatchObject({ isChampion: true, title: null, kda: '5/6/7' });
    expect(rows[1]).toMatchObject({ isChampion: false, title: 'SCOUT' });
    expect(rows[1].sideKey).toBe('playerProfile.matches.detail.otherTeam');
  });
});

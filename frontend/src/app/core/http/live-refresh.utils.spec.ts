import { describe, expect, it } from 'vitest';

import { PlayerSummary } from '@core/players/player-summary.model';

import { liveRefreshStamp } from './live-refresh.utils';

function player(lastSuccessfulSynchronizationAt: string | null): PlayerSummary {
  return {
    id: 1,
    riotId: 'Op#EUW',
    displayName: 'Op',
    portrait: null,
    competitiveTier: 'UNRANKED',
    rankRating: null,
    kda: null,
    winRate: null,
    headshotPercentage: null,
    matchesPlayed: 0,
    status: 'ACTIVE',
    lastSuccessfulSynchronizationAt,
  } as PlayerSummary;
}

const NOON = new Date(2026, 8, 7, 12, 0, 0);

describe('liveRefreshStamp', () => {
  it('changes when a player synchronizes again', () => {
    const before = liveRefreshStamp([player('2026-09-07T09:00:00Z')], NOON);
    const after = liveRefreshStamp([player('2026-09-07T09:30:00Z')], NOON);

    expect(after).not.toBe(before);
  });

  it('stays the same while nothing has synchronized and the day has not turned', () => {
    const players = [player('2026-09-07T09:00:00Z')];

    expect(liveRefreshStamp(players, NOON)).toBe(
      liveRefreshStamp(players, new Date(2026, 8, 7, 18, 45, 0)),
    );
  });

  it('turns the day a quarter of an hour after local midnight, once the nightly tick is over', () => {
    const players = [player('2026-09-07T22:00:00Z')];
    const lateEvening = liveRefreshStamp(players, new Date(2026, 8, 7, 23, 59, 0));

    expect(liveRefreshStamp(players, new Date(2026, 8, 8, 0, 14, 0))).toBe(lateEvening);
    expect(liveRefreshStamp(players, new Date(2026, 8, 8, 0, 16, 0))).not.toBe(lateEvening);
  });

  it('reads a roster nobody has synchronized as one stamp per day', () => {
    expect(liveRefreshStamp([player(null)], NOON)).toBe(liveRefreshStamp([], NOON));
  });
});

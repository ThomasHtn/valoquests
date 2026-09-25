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

// Explicit instants: Paris is UTC+2 in September, whatever the runtime zone.
const NOON = new Date('2026-09-07T10:00:00Z');

describe('liveRefreshStamp', () => {
  it('changes when a player synchronizes again', () => {
    const before = liveRefreshStamp([player('2026-09-07T09:00:00Z')], NOON);
    const after = liveRefreshStamp([player('2026-09-07T09:30:00Z')], NOON);

    expect(after).not.toBe(before);
  });

  it('stays the same while nothing has synchronized and the day has not turned', () => {
    const players = [player('2026-09-07T09:00:00Z')];

    expect(liveRefreshStamp(players, NOON)).toBe(
      liveRefreshStamp(players, new Date('2026-09-07T16:45:00Z')),
    );
  });

  it('turns the day a quarter of an hour after Paris midnight, once the nightly tick is over', () => {
    const players = [player('2026-09-07T20:00:00Z')];
    const lateEvening = liveRefreshStamp(players, new Date('2026-09-07T21:59:00Z'));

    expect(liveRefreshStamp(players, new Date('2026-09-07T22:14:00Z'))).toBe(lateEvening);
    expect(liveRefreshStamp(players, new Date('2026-09-07T22:16:00Z'))).not.toBe(lateEvening);
  });

  it('reads a roster nobody has synchronized as one stamp per day', () => {
    expect(liveRefreshStamp([player(null)], NOON)).toBe(liveRefreshStamp([], NOON));
  });
});

import { describe, expect, it } from 'vitest';

import { liveRefreshStamp } from './live-refresh.utils';

// Explicit instants: Paris is UTC+2 in September, whatever the runtime zone.
const NOON = new Date('2026-09-07T10:00:00Z');

describe('liveRefreshStamp', () => {
  it('changes when a synchronization finishes again', () => {
    const before = liveRefreshStamp('2026-09-07T09:00:00Z', NOON);
    const after = liveRefreshStamp('2026-09-07T09:30:00Z', NOON);

    expect(after).not.toBe(before);
  });

  it('stays the same while nothing has finished and the day has not turned', () => {
    expect(liveRefreshStamp('2026-09-07T09:00:00Z', NOON)).toBe(
      liveRefreshStamp('2026-09-07T09:00:00Z', new Date('2026-09-07T16:45:00Z')),
    );
  });

  it('turns the day a quarter of an hour after Paris midnight, once the nightly tick is over', () => {
    const lastCompletedAt = '2026-09-07T20:00:00Z';
    const lateEvening = liveRefreshStamp(lastCompletedAt, new Date('2026-09-07T21:59:00Z'));

    expect(liveRefreshStamp(lastCompletedAt, new Date('2026-09-07T22:14:00Z'))).toBe(lateEvening);
    expect(liveRefreshStamp(lastCompletedAt, new Date('2026-09-07T22:16:00Z'))).not.toBe(
      lateEvening,
    );
  });
});

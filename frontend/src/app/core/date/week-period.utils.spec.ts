import { describe, expect, it } from 'vitest';

import { remainingWeekTime } from './week-period.utils';

describe('remainingWeekTime', () => {
  it('counts down to Monday 00:00 Paris, whatever the runtime zone', () => {
    // Sunday 2026-09-13 23:00 Paris is 21:00 UTC: one hour left.
    expect(remainingWeekTime('2026-09-13', new Date('2026-09-13T21:00:00Z'))).toEqual({
      days: 0,
      hours: 1,
      minutes: 0,
    });
  });

  it('reaches zero at the Paris rollover and stays there', () => {
    const zero = { days: 0, hours: 0, minutes: 0 };
    expect(remainingWeekTime('2026-09-13', new Date('2026-09-13T22:00:00Z'))).toEqual(zero);
    expect(remainingWeekTime('2026-09-13', new Date('2026-09-13T23:30:00Z'))).toEqual(zero);
  });

  it('accounts for the daylight-saving change inside the week of 2026-10-25', () => {
    // Monday 2026-10-26 00:00 Paris is 2026-10-25T23:00Z (back to UTC+1).
    expect(remainingWeekTime('2026-10-25', new Date('2026-10-24T23:00:00Z'))).toEqual({
      days: 1,
      hours: 0,
      minutes: 0,
    });
  });
});

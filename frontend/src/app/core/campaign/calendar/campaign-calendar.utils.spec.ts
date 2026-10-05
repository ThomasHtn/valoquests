import { describe, expect, it } from 'vitest';

import {
  campaignMidnight,
  remainingWeekTime,
  toCampaignDayKey,
  weekDayIndex,
} from './campaign-calendar.utils';

// Explicit UTC instants keep these assertions independent of the runtime time zone.
describe('campaignMidnight', () => {
  it('resolves Paris midnight in summer time (UTC+2)', () => {
    expect(campaignMidnight('2026-09-07').toISOString()).toBe('2026-09-06T22:00:00.000Z');
  });

  it('resolves Paris midnight in winter time (UTC+1)', () => {
    expect(campaignMidnight('2026-12-14').toISOString()).toBe('2026-12-13T23:00:00.000Z');
  });

  it('crosses the autumn daylight-saving change of 2026-10-25', () => {
    expect(campaignMidnight('2026-10-25').toISOString()).toBe('2026-10-24T22:00:00.000Z');
    expect(campaignMidnight('2026-10-25', 1).toISOString()).toBe('2026-10-25T23:00:00.000Z');
    expect(campaignMidnight('2026-10-19', 7).toISOString()).toBe('2026-10-25T23:00:00.000Z');
  });

  it('crosses the spring daylight-saving change of 2026-03-29', () => {
    expect(campaignMidnight('2026-03-29').toISOString()).toBe('2026-03-28T23:00:00.000Z');
    expect(campaignMidnight('2026-03-30').toISOString()).toBe('2026-03-29T22:00:00.000Z');
  });

  it('rolls over month and year ends', () => {
    expect(campaignMidnight('2026-12-31', 1).toISOString()).toBe('2026-12-31T23:00:00.000Z');
  });
});

describe('toCampaignDayKey', () => {
  it('splits days at Paris midnight, not UTC midnight', () => {
    expect(toCampaignDayKey('2026-09-07T21:59:59Z')).toBe('2026-09-07');
    expect(toCampaignDayKey('2026-09-07T22:00:00Z')).toBe('2026-09-08');
  });

  it('follows the offset change around 2026-10-25', () => {
    expect(toCampaignDayKey('2026-10-24T21:59:00Z')).toBe('2026-10-24');
    expect(toCampaignDayKey('2026-10-24T22:00:00Z')).toBe('2026-10-25');
    expect(toCampaignDayKey('2026-10-25T22:59:00Z')).toBe('2026-10-25');
    expect(toCampaignDayKey('2026-10-25T23:00:00Z')).toBe('2026-10-26');
  });
});

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

describe('weekDayIndex', () => {
  it('counts days from Monday', () => {
    expect(weekDayIndex('2026-10-05', '2026-10-07')).toBe(2);
  });

  it('clamps to the seven days of the week', () => {
    expect(weekDayIndex('2026-10-05', '2026-10-04')).toBe(0);
    expect(weekDayIndex('2026-10-05', '2026-10-14')).toBe(6);
  });
});

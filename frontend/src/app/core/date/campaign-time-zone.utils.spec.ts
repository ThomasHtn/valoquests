import { describe, expect, it } from 'vitest';

import { campaignMidnight, toCampaignDayKey } from './campaign-time-zone.utils';

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

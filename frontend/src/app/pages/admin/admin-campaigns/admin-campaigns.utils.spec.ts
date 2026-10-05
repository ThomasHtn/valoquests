import { describe, expect, it } from 'vitest';

import { Campaign } from '@core/campaign/campaign.model';
import { buildLiveCampaign } from './admin-campaigns.utils';

/**
 * Running campaign on its third day, overridable per test.
 */
function campaign(overrides: Partial<Campaign> = {}): Campaign {
  return {
    id: 4,
    status: 'RUNNING',
    number: 2,
    difficulty: 'PRO',
    reference: 5300,
    rosterSize: 6,
    firstWeekStart: '2026-09-07',
    lastWeekStart: '2026-11-09',
    today: '2026-09-09',
    currentWeekIndex: 1,
    base: null,
    forecast: null,
    weeks: [],
    totals: null,
    ...overrides,
  };
}

describe('buildLiveCampaign', () => {
  it('returns nothing between campaigns or once closed', () => {
    expect(buildLiveCampaign(null)).toBeNull();
    expect(buildLiveCampaign(campaign({ id: null }))).toBeNull();
    expect(buildLiveCampaign(campaign({ status: 'CLOSED' }))).toBeNull();
    expect(buildLiveCampaign(campaign({ firstWeekStart: null }))).toBeNull();
  });

  it('counts days from the first Monday, which is day one', () => {
    expect(buildLiveCampaign(campaign())).toMatchObject({
      id: 4,
      number: 2,
      difficulty: 'PRO',
      weekIndex: 1,
      dayIndex: 3,
      daysLeft: 67,
    });
  });

  it('clamps the day to zero before the start and to the last day after the end', () => {
    expect(buildLiveCampaign(campaign({ status: 'OPENED', today: '2026-09-01' }))).toMatchObject({
      dayIndex: 0,
      daysLeft: 70,
    });
    expect(buildLiveCampaign(campaign({ today: '2027-01-01' }))).toMatchObject({
      dayIndex: 70,
      daysLeft: 0,
    });
  });
});

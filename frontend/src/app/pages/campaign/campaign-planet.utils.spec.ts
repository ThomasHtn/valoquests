import { describe, expect, it } from 'vitest';

import { Campaign, CampaignWeek } from '@core/campaign/campaign.model';
import { padCurve, resolvePlanetState, resolveSeasonKey } from './campaign-planet.utils';

function week(overrides: Partial<CampaignWeek>): CampaignWeek {
  return { weekIndex: 1, defeated: false, settled: false, ...overrides } as CampaignWeek;
}

function campaign(overrides: Partial<Campaign>): Campaign {
  return { status: 'RUNNING', currentWeekIndex: 3, ...overrides } as Campaign;
}

describe('resolvePlanetState', () => {
  it('marks a defeated guardian as won, whatever else the week says', () => {
    expect(resolvePlanetState(week({ defeated: true, settled: true }), campaign({}))).toBe('won');
  });

  it('marks a settled week without a defeat as lost', () => {
    expect(resolvePlanetState(week({ settled: true }), campaign({}))).toBe('lost');
  });

  it('marks the running campaign’s current week as now', () => {
    expect(resolvePlanetState(week({ weekIndex: 3 }), campaign({}))).toBe('now');
  });

  it('keeps every other week ahead, including the current one of a closed campaign', () => {
    expect(resolvePlanetState(week({ weekIndex: 4 }), campaign({}))).toBe('ahead');
    expect(resolvePlanetState(week({ weekIndex: 3 }), campaign({ status: 'CLOSED' }))).toBe(
      'ahead',
    );
  });
});

describe('padCurve', () => {
  it('pads a short curve with gaps up to the ten weeks', () => {
    const padded = padCurve([10, 20]);

    expect(padded).toHaveLength(10);
    expect(padded.slice(0, 3)).toEqual([10, 20, null]);
  });
});

describe('resolveSeasonKey', () => {
  it('maps every month to its season', () => {
    expect(resolveSeasonKey(new Date(2026, 0, 15))).toBe('winter');
    expect(resolveSeasonKey(new Date(2026, 11, 15))).toBe('winter');
    expect(resolveSeasonKey(new Date(2026, 3, 15))).toBe('spring');
    expect(resolveSeasonKey(new Date(2026, 6, 15))).toBe('summer');
    expect(resolveSeasonKey(new Date(2026, 9, 15))).toBe('autumn');
  });
});

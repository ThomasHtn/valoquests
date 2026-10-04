/**
 * Test-only factories shared by the overview's specs.
 */
import { Campaign, CampaignBase } from '@core/campaign/campaign.model';
import { CampaignWeek } from '@core/campaign/campaign-week.model';
import { TranslateFn } from '@core/i18n/translation.model';
import { PlayerSummary } from '@core/players/player-summary.model';

/**
 * Fake translator echoing the key and its parameters, so specs can assert both.
 */
export const translate: TranslateFn = (key, params) =>
  params ? `${key}(${Object.values(params).join(',')})` : key;

/**
 * Unsettled first week of a running campaign, overridable field by field.
 */
export function week(overrides: Partial<CampaignWeek> = {}): CampaignWeek {
  return {
    weekIndex: 1,
    weekStart: '2026-01-05',
    planetName: 'Kepler',
    category: 'STANDARD',
    guardianName: 'Vex',
    guardianDescription: null,
    guardianHitPoints: 1000,
    damageDealt: 0,
    dailyDamage: [],
    progressPercent: 0,
    defeated: false,
    defeatedAt: null,
    defeatedByPlayerId: null,
    fatalBlow: null,
    woundedCount: 10,
    challengeRescued: 0,
    extractionRescued: 0,
    foodSpent: 0,
    componentsSpent: 0,
    limiter: 'NONE',
    baseLoss: 0,
    settled: false,
    base: null,
    ...overrides,
  };
}

/**
 * Base with small round stocks and rates, overridable field by field.
 */
export function base(overrides: Partial<CampaignBase> = {}): CampaignBase {
  return {
    population: 1000,
    foodStock: 100,
    componentsStock: 100,
    dailyUpkeep: 10,
    protectedFood: 10,
    rescuesByComponents: 5,
    rescuesByFood: 5,
    populationChange: 0,
    componentsPerRescue: 2,
    foodPerRescue: 2,
    guardianLossPercent: 5,
    ...overrides,
  };
}

/**
 * Running campaign in its first week, overridable field by field.
 */
export function campaign(overrides: Partial<Campaign> = {}): Campaign {
  return {
    id: 1,
    status: 'RUNNING',
    number: 3,
    difficulty: 'AMATEUR',
    reference: 1000,
    rosterSize: 5,
    firstWeekStart: '2026-01-05',
    lastWeekStart: '2026-03-09',
    today: '2026-01-05',
    currentWeekIndex: 1,
    base: base(),
    forecast: null,
    weeks: [week()],
    totals: null,
    ...overrides,
  };
}

/**
 * Active squad member, overridable field by field.
 */
export function player(overrides: Partial<PlayerSummary> = {}): PlayerSummary {
  return {
    id: 1,
    riotId: 'Player#EU1',
    displayName: 'Player',
    portrait: null,
    competitiveTier: 'GOLD_1',
    rankRating: null,
    kda: null,
    winRate: null,
    headshotPercentage: null,
    matchesPlayed: 10,
    status: 'ACTIVE',
    lastSuccessfulSynchronizationAt: null,
    ...overrides,
  };
}

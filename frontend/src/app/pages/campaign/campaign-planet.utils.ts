import { Campaign, CAMPAIGN_WEEK_COUNT, CampaignWeek } from '@core/campaign/campaign.model';
import { PlanetState } from './campaign.model';

/**
 * Pure helpers placing a week on the road of the planets.
 */

/**
 * Where a week stands: won, lost, the one being played, or still ahead.
 */
export function resolvePlanetState(week: CampaignWeek, campaign: Campaign): PlanetState {
  if (week.defeated) {
    return 'won';
  }
  if (week.settled) {
    return 'lost';
  }
  return week.weekIndex === campaign.currentWeekIndex && campaign.status === 'RUNNING'
    ? 'now'
    : 'ahead';
}

/**
 * Pads a curve with trailing gaps so every campaign spans the ten weeks.
 */
export function padCurve(points: readonly (number | null)[]): readonly (number | null)[] {
  return Array.from({ length: CAMPAIGN_WEEK_COUNT }, (_, index) => points[index] ?? null);
}

/**
 * Season a date falls in, as a translation key suffix.
 */
export function resolveSeasonKey(date: Date): 'winter' | 'spring' | 'summer' | 'autumn' {
  const month = date.getMonth();
  if (month <= 1 || month === 11) {
    return 'winter';
  }
  if (month <= 4) {
    return 'spring';
  }
  return month <= 7 ? 'summer' : 'autumn';
}

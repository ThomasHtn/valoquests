import { Campaign } from '@core/campaign/campaign.model';
import { CampaignWeek } from '@core/campaign/campaign-week.model';
import { HULL_FIGURE_MIN_SIZE, HULL_FIGURE_SIZES } from './extraction-gauges.constants';
import { Capacity } from './extraction-gauges.model';

/**
 * CSS length for `--fig` that keeps the aboard figure inside the hull.
 */
export function hullFigureSize(value: number): string {
  const digits = String(Math.max(0, Math.round(value))).length;
  return HULL_FIGURE_SIZES.find((step) => digits <= step.maxDigits)?.size ?? HULL_FIGURE_MIN_SIZE;
}

/**
 * Four extraction dials, `null` outside a running week with a forecast.
 */
export function buildCapacity(
  campaign: Campaign | null,
  week: CampaignWeek | null,
): Capacity | null {
  const base = campaign?.base;
  const forecast = campaign?.forecast;
  if (!campaign || !week || !base || !forecast) {
    return null;
  }
  const wounded = Math.max(1, week.woundedCount);
  const fraction = (value: number): number => Math.min(1, value / wounded);
  return {
    wounded: week.woundedCount,
    carry: {
      value: base.rescuesByComponents,
      fraction: fraction(base.rescuesByComponents),
      stock: base.componentsStock,
    },
    shelter: {
      value: base.rescuesByFood,
      fraction: fraction(base.rescuesByFood),
      stock: base.foodStock,
    },
    breach: {
      value: week.progressPercent,
      fraction: week.progressPercent / 100,
      stock: Math.max(0, week.guardianHitPoints - week.damageDealt),
    },
    aboard: forecast.rescued,
    aboardFraction: fraction(forecast.rescued),
    fromGuardian: forecast.extractionRescued,
    fromChallenges: forecast.challengeRescued,
    leftBehind: forecast.leftBehind,
    limiter: forecast.limiter,
    componentsPerRescue: base.componentsPerRescue,
    foodPerRescue: base.foodPerRescue,
    hitPointsPerPercent: Math.round(week.guardianHitPoints / 100),
  };
}

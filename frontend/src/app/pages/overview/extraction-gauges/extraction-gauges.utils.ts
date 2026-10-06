import { LucideBuilding2 } from '@lucide/angular';

import { Campaign } from '@core/campaign/campaign.model';
import { CampaignWeek } from '@core/campaign/campaign-week.model';
import { CONCEPT_ICONS } from '@core/concepts/concept.constants';
import { TranslateFn } from '@core/i18n/translation.model';

import {
  CARRY_MODES,
  HULL_FIGURE_MIN_SIZE,
  HULL_FIGURE_SIZES,
  SHELTER_MODES,
} from './extraction-gauges.constants';
import { Capacity, LimitDial, LimitDialKey } from './extraction-gauges.model';

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

/**
 * The three limiting dials (carry, shelter, breakthrough) in reading order.
 * @param bossLabel `Boss 04`, which names the breakthrough dial's guardian.
 */
export function buildLimitDials(
  capacity: Capacity,
  bossLabel: string,
  translate: TranslateFn,
  format: (amount: number) => string,
): readonly LimitDial[] {
  const percent = (fraction: number): number => Math.round(fraction * 100);
  const named = (key: LimitDialKey) => {
    const name = translate(`overview.capacity.${key}`);
    return { key, name, infoLabel: translate('overview.capacity.info', { name }) };
  };
  const modes = (keys: readonly string[]): readonly string[] =>
    keys.map((mode) => translate(`common.gameMode.${mode}`));
  const reading = (count: number, fraction: number) => ({
    count: format(count),
    wounded: format(capacity.wounded),
    percent: percent(fraction),
  });
  const { carry, shelter, breach } = capacity;
  return [
    {
      ...named('carry'),
      tooltip: translate('overview.capacity.carryTooltip', { rate: capacity.componentsPerRescue }),
      ariaLabel: translate('overview.capacity.carryAria', reading(carry.value, carry.fraction)),
      fraction: carry.fraction,
      value: carry.value,
      unit: '',
      icon: CONCEPT_ICONS.rocket,
      resourceIcon: CONCEPT_ICONS.components,
      stock: format(carry.stock),
      stockLabel: translate('common.resource.components').toLowerCase(),
      modes: modes(CARRY_MODES),
      rate: String(capacity.componentsPerRescue),
      rateLabel: translate('overview.capacity.carryRate'),
    },
    {
      ...named('shelter'),
      tooltip: translate('overview.capacity.shelterTooltip', { rate: capacity.foodPerRescue }),
      ariaLabel: translate(
        'overview.capacity.shelterAria',
        reading(shelter.value, shelter.fraction),
      ),
      fraction: shelter.fraction,
      value: shelter.value,
      unit: '',
      icon: LucideBuilding2,
      resourceIcon: CONCEPT_ICONS.food,
      stock: format(shelter.stock),
      stockLabel: translate('common.resource.food').toLowerCase(),
      modes: modes(SHELTER_MODES),
      rate: String(capacity.foodPerRescue),
      rateLabel: translate('overview.capacity.shelterRate'),
    },
    {
      ...named('breach'),
      tooltip: translate('overview.capacity.breachTooltip'),
      ariaLabel: translate('overview.capacity.breachAria', {
        boss: bossLabel,
        percent: breach.value,
      }),
      fraction: breach.fraction,
      value: breach.value,
      unit: ' %',
      icon: CONCEPT_ICONS.damage,
      resourceIcon: CONCEPT_ICONS.guardian,
      stock: format(breach.stock),
      stockLabel: translate('overview.capacity.hpStanding'),
      modes: [translate('overview.capacity.allModes')],
      rate: format(capacity.hitPointsPerPercent),
      rateLabel: translate('overview.capacity.breachRate'),
    },
  ];
}

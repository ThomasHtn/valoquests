import { TranslateFn } from '@core/i18n/translation.model';

import {
  COMPETITIVE_TIER_GROUPS,
  COMPETITIVE_TIER_ORDER,
  TIER_GROUP_COLOR_TOKENS,
} from './player-competitive-tier.constants';
import { CompetitiveTier, CompetitiveTierVisual, TierGroup } from './player-competitive-tier.model';

/**
 * Rank group of a tier, `undefined` for a tier the backend added since this build.
 */
function findTierGroup(tier: CompetitiveTier): TierGroup | undefined {
  return COMPETITIVE_TIER_GROUPS[tier];
}

/**
 * Zero-based position of a tier on the ladder, lowest first.
 */
export function resolveTierOrdinal(tier: CompetitiveTier): number {
  return COMPETITIVE_TIER_ORDER.indexOf(tier);
}

/**
 * Translated label (e.g. `"Diamant 2"`) and colour of a tier, unranked for an unknown one.
 */
export function resolveCompetitiveTierVisual(
  tier: CompetitiveTier,
  translate: TranslateFn,
): CompetitiveTierVisual {
  const group = findTierGroup(tier) ?? COMPETITIVE_TIER_GROUPS.UNRANKED;
  const groupLabel = translate(`players.tiers.${group.key}`);

  return {
    label: group.number ? `${groupLabel} ${group.number}` : groupLabel,
    tone: `var(--color-${TIER_GROUP_COLOR_TOKENS[group.key]})`,
  };
}

/**
 * Path of a tier's `public/ranks` SVG, `null` for an unknown tier.
 */
export function resolveCompetitiveTierIconUrl(tier: CompetitiveTier): string | null {
  const group = findTierGroup(tier);
  if (!group) {
    return null;
  }

  const filename = group.number ? `${group.key}-${group.number}` : group.key;
  return `/ranks/${filename}.svg`;
}

/**
 * CSS variable of a tier's colour (e.g. `--color-accent-gold`), for charts that read tokens.
 */
export function resolveCompetitiveTierColorVariable(tier: CompetitiveTier): string {
  return `--color-${TIER_GROUP_COLOR_TOKENS[findTierGroup(tier)?.key ?? 'unranked']}`;
}

/**
 * Tier at a zero-based ladder position, `null` past either end.
 */
export function resolveTierFromOrdinal(ordinal: number): CompetitiveTier | null {
  return COMPETITIVE_TIER_ORDER[ordinal] ?? null;
}

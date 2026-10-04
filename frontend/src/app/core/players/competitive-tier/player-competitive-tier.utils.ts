import {
  COMPETITIVE_TIER_GROUPS,
  COMPETITIVE_TIER_ORDER,
  TIER_GROUP_COLOR_CLASSES,
} from './player-competitive-tier.constants';
import { CompetitiveTier, CompetitiveTierVisual } from './player-competitive-tier.model';

/**
 * Zero-based position of a tier on the ladder, lowest first.
 */
export function resolveTierOrdinal(tier: CompetitiveTier): number {
  return COMPETITIVE_TIER_ORDER.indexOf(tier);
}

/**
 * Translated label (e.g. `"Diamant 2"`) and colour class of a tier.
 */
export function resolveCompetitiveTierVisual(
  tier: CompetitiveTier,
  translate: (key: string) => string,
): CompetitiveTierVisual {
  const group = COMPETITIVE_TIER_GROUPS[tier];
  const groupLabel = translate(`players.tiers.${group.key}`);

  return {
    label: group.number ? `${groupLabel} ${group.number}` : groupLabel,
    colorClass: TIER_GROUP_COLOR_CLASSES[group.key] ?? TIER_GROUP_COLOR_CLASSES['unranked'],
  };
}

/**
 * Path of a tier's `public/ranks` SVG, `null` for an unknown tier.
 */
export function resolveCompetitiveTierIconUrl(tier: CompetitiveTier): string | null {
  const group = COMPETITIVE_TIER_GROUPS[tier];
  if (!group) {
    return null;
  }

  const filename = group.number ? `${group.key}-${group.number}` : group.key;
  return `/ranks/${filename}.svg`;
}

/**
 * CSS variable of a tier's colour (e.g. `--color-accent-gold`), for charts that take no class.
 */
export function resolveCompetitiveTierColorVariable(tier: CompetitiveTier): string {
  const group = COMPETITIVE_TIER_GROUPS[tier];
  const colorClass =
    TIER_GROUP_COLOR_CLASSES[group?.key ?? 'unranked'] ?? TIER_GROUP_COLOR_CLASSES['unranked'];
  return `--color-${colorClass.replace(/^text-/, '')}`;
}

/**
 * Tier at a zero-based ladder position, `null` past either end.
 */
export function resolveTierFromOrdinal(ordinal: number): CompetitiveTier | null {
  return COMPETITIVE_TIER_ORDER[ordinal] ?? null;
}

import { CompetitiveTier, CompetitiveTierVisual } from './competitive-tier.model';

/**
 * Rank group a {@link CompetitiveTier} belongs to (e.g. `DIAMOND_2` belongs to `"diamond"`), paired
 * with its sub-rank number, or `null` for groups with a single rank (`UNRANKED`, `RADIANT`).
 *
 * The group key doubles as the suffix of a `players.tiers.*` translation key and as the lookup key
 * into {@link TIER_GROUP_COLOR_CLASSES}.
 */
interface TierGroup {
  readonly key: string;
  readonly number: number | null;
}

/**
 * Maps every {@link CompetitiveTier} to its rank group and sub-rank number.
 */
const COMPETITIVE_TIER_GROUPS: Readonly<Record<CompetitiveTier, TierGroup>> = {
  UNRANKED: { key: 'unranked', number: null },
  IRON_1: { key: 'iron', number: 1 },
  IRON_2: { key: 'iron', number: 2 },
  IRON_3: { key: 'iron', number: 3 },
  BRONZE_1: { key: 'bronze', number: 1 },
  BRONZE_2: { key: 'bronze', number: 2 },
  BRONZE_3: { key: 'bronze', number: 3 },
  SILVER_1: { key: 'silver', number: 1 },
  SILVER_2: { key: 'silver', number: 2 },
  SILVER_3: { key: 'silver', number: 3 },
  GOLD_1: { key: 'gold', number: 1 },
  GOLD_2: { key: 'gold', number: 2 },
  GOLD_3: { key: 'gold', number: 3 },
  PLATINUM_1: { key: 'platinum', number: 1 },
  PLATINUM_2: { key: 'platinum', number: 2 },
  PLATINUM_3: { key: 'platinum', number: 3 },
  DIAMOND_1: { key: 'diamond', number: 1 },
  DIAMOND_2: { key: 'diamond', number: 2 },
  DIAMOND_3: { key: 'diamond', number: 3 },
  ASCENDANT_1: { key: 'ascendant', number: 1 },
  ASCENDANT_2: { key: 'ascendant', number: 2 },
  ASCENDANT_3: { key: 'ascendant', number: 3 },
  IMMORTAL_1: { key: 'immortal', number: 1 },
  IMMORTAL_2: { key: 'immortal', number: 2 },
  IMMORTAL_3: { key: 'immortal', number: 3 },
  RADIANT: { key: 'radiant', number: null },
};

/**
 * Every {@link CompetitiveTier}, from lowest to highest, used to rank players by tier before
 * comparing their in-tier rank rating (which resets per tier and is otherwise not comparable
 * across tiers).
 */
const COMPETITIVE_TIER_ORDER: readonly CompetitiveTier[] = [
  'UNRANKED',
  'IRON_1',
  'IRON_2',
  'IRON_3',
  'BRONZE_1',
  'BRONZE_2',
  'BRONZE_3',
  'SILVER_1',
  'SILVER_2',
  'SILVER_3',
  'GOLD_1',
  'GOLD_2',
  'GOLD_3',
  'PLATINUM_1',
  'PLATINUM_2',
  'PLATINUM_3',
  'DIAMOND_1',
  'DIAMOND_2',
  'DIAMOND_3',
  'ASCENDANT_1',
  'ASCENDANT_2',
  'ASCENDANT_3',
  'IMMORTAL_1',
  'IMMORTAL_2',
  'IMMORTAL_3',
  'RADIANT',
];

/**
 * Text/badge color applied per rank group, reusing the application's existing accent palette so no
 * new colors are introduced.
 */
const TIER_GROUP_COLOR_CLASSES: Readonly<Record<string, string>> = {
  unranked: 'text-text-muted',
  iron: 'text-text-muted',
  bronze: 'text-accent-red',
  silver: 'text-text-secondary',
  gold: 'text-accent-gold',
  platinum: 'text-accent-cyan',
  diamond: 'text-accent-purple',
  ascendant: 'text-accent-green',
  immortal: 'text-accent-pink',
  radiant: 'text-accent-blue',
};

/**
 * Resolves a competitive tier's position among all tiers, from lowest (`0`) to highest, so players
 * can be ranked by tier before their in-tier rank rating.
 *
 * @param tier - The player's competitive tier.
 * @returns The tier's 0-based ordinal position.
 */
export function resolveTierOrdinal(tier: CompetitiveTier): number {
  return COMPETITIVE_TIER_ORDER.indexOf(tier);
}

/**
 * Resolves the translated label and color class for a player's competitive tier, e.g.
 * `"Diamant 2"` paired with the color shared by the rank's badge and text.
 *
 * Takes a translator rather than injecting the i18n service so it stays a pure function of its
 * inputs, and so both the players list and the player profile render ranks identically.
 *
 * @param tier - The player's competitive tier.
 * @param translate - Resolves a `players.tiers.*` key into the active language.
 * @returns The tier's display-ready label and color class.
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
 * Resolves the SVG icon path for a competitive tier, used by rank icon components to display
 * tier badge visuals via NgOptimizedImage.
 *
 * Returns a path to `public/ranks/{tier-key}[-{number}].svg` matching the tier's group key
 * and optional sub-rank number, or `null` if the tier cannot be resolved.
 *
 * @param tier - The player's competitive tier.
 * @returns The relative path to the tier's SVG icon in the public folder, or `null` if unresolvable.
 */
export function resolveCompetitiveTierIconUrl(tier: CompetitiveTier): string | null {
  const group = COMPETITIVE_TIER_GROUPS[tier];
  if (!group) {
    return null;
  }

  const filename = group.number ? `${group.key}-${group.number}` : group.key;
  return `/ranks/${filename}.svg`;
}

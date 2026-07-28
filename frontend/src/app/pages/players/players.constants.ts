import { CompetitiveTier } from '../../core/players/competitive-tier.model';

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
 * Threshold above which a win rate or KDA is considered good enough to be highlighted in green
 * rather than gold, mirroring the challenge difficulty color language (green for favorable, gold
 * for attention).
 */
const WIN_RATE_GOOD_THRESHOLD = 50;
const KDA_GOOD_THRESHOLD = 1.3;

/**
 * Resolves the rank group and sub-rank number for a competitive tier.
 *
 * @param tier - The player's competitive tier.
 * @returns The tier's rank group and sub-rank number.
 */
export function resolveTierGroup(tier: CompetitiveTier): TierGroup {
  return COMPETITIVE_TIER_GROUPS[tier];
}

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
 * Resolves the color class for a rank group.
 *
 * @param groupKey - A rank group key, as returned by {@link resolveTierGroup}.
 * @returns The Tailwind text color utility to apply.
 */
export function resolveTierColorClass(groupKey: string): string {
  return TIER_GROUP_COLOR_CLASSES[groupKey] ?? TIER_GROUP_COLOR_CLASSES['unranked'];
}

/**
 * Resolves the color class for a win rate value.
 *
 * @param winRate - The player's win rate percentage, or `null` when not yet synchronized.
 * @returns The Tailwind text/background color utility to apply.
 */
export function resolveWinRateColorClass(winRate: number | null): string {
  if (winRate === null) {
    return 'text-text-secondary';
  }
  return winRate >= WIN_RATE_GOOD_THRESHOLD ? 'text-accent-green' : 'text-accent-gold';
}

/**
 * Resolves the progress bar fill color for a win rate value, paired with
 * {@link resolveWinRateColorClass}.
 *
 * @param winRate - The player's win rate percentage, or `null` when not yet synchronized.
 * @returns The Tailwind background color utility to apply to the bar's fill.
 */
export function resolveWinRateBarClass(winRate: number | null): string {
  if (winRate === null) {
    return 'bg-text-secondary';
  }
  return winRate >= WIN_RATE_GOOD_THRESHOLD ? 'bg-accent-green' : 'bg-accent-gold';
}

/**
 * Resolves the color class for a KDA value.
 *
 * @param kda - The player's KDA ratio, or `null` when not yet synchronized.
 * @returns The Tailwind text color utility to apply.
 */
export function resolveKdaColorClass(kda: number | null): string {
  if (kda === null) {
    return 'text-text-secondary';
  }
  return kda >= KDA_GOOD_THRESHOLD ? 'text-accent-green' : 'text-accent-gold';
}

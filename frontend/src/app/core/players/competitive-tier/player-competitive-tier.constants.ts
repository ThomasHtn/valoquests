import { CompetitiveTier, TierGroup } from './player-competitive-tier.model';

/**
 * Maps every {@link CompetitiveTier} to its rank group and sub-rank number.
 */
export const COMPETITIVE_TIER_GROUPS: Readonly<Record<CompetitiveTier, TierGroup>> = {
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
 * Every {@link CompetitiveTier}, lowest first; ranks players before their in-tier rating.
 */
export const COMPETITIVE_TIER_ORDER: readonly CompetitiveTier[] = [
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
 * Text and badge colour per rank group, from the accent palette.
 */
export const TIER_GROUP_COLOR_CLASSES: Readonly<Record<string, string>> = {
  unranked: 'text-text-muted',
  iron: 'text-text-muted',
  bronze: 'text-podium-bronze',
  silver: 'text-text-secondary',
  gold: 'text-accent-gold',
  platinum: 'text-accent-cyan',
  diamond: 'text-accent-purple',
  ascendant: 'text-accent-green',
  immortal: 'text-accent-pink',
  radiant: 'text-accent-blue',
};

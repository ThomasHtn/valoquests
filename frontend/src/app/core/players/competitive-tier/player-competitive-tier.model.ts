/**
 * Competitive rank tier; mirrors the backend `CompetitiveTier`.
 */
export type CompetitiveTier =
  | 'UNRANKED'
  | 'IRON_1'
  | 'IRON_2'
  | 'IRON_3'
  | 'BRONZE_1'
  | 'BRONZE_2'
  | 'BRONZE_3'
  | 'SILVER_1'
  | 'SILVER_2'
  | 'SILVER_3'
  | 'GOLD_1'
  | 'GOLD_2'
  | 'GOLD_3'
  | 'PLATINUM_1'
  | 'PLATINUM_2'
  | 'PLATINUM_3'
  | 'DIAMOND_1'
  | 'DIAMOND_2'
  | 'DIAMOND_3'
  | 'ASCENDANT_1'
  | 'ASCENDANT_2'
  | 'ASCENDANT_3'
  | 'IMMORTAL_1'
  | 'IMMORTAL_2'
  | 'IMMORTAL_3'
  | 'RADIANT';

/**
 * Translated label and colour of a competitive tier.
 */
export interface CompetitiveTierVisual {
  /**
   * Translated tier name.
   */
  readonly label: string;

  /**
   * CSS colour of the tier (`var(--color-accent-gold)`), bound as `--tone`.
   */
  readonly tone: string;
}

/**
 * Rank group a tier belongs to, also its translation and icon key.
 */
export type TierGroupKey =
  | 'unranked'
  | 'iron'
  | 'bronze'
  | 'silver'
  | 'gold'
  | 'platinum'
  | 'diamond'
  | 'ascendant'
  | 'immortal'
  | 'radiant';

/**
 * Rank group of a {@link CompetitiveTier} (`DIAMOND_2` is `diamond`, 2).
 */
export interface TierGroup {
  /**
   * Group key (`diamond`).
   */
  readonly key: TierGroupKey;

  /**
   * Division within the group, `null` for single-division tiers.
   */
  readonly number: number | null;
}

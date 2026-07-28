/**
 * Competitive rank tiers supported by the application.
 *
 * Mirrors the backend `CompetitiveTier` enum.
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
 * Resolved visual treatment for a player's competitive tier: a translated label (e.g.
 * `"Diamond 1"`) paired with the color class shared by its badge and text.
 */
export interface CompetitiveTierVisual {
  readonly label: string;
  readonly colorClass: string;
}

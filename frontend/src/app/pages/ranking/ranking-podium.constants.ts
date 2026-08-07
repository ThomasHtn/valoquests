/**
 * Visual treatment for one podium tier (1st, 2nd or 3rd place): the card's gradient tint and the
 * physical plinth block it stands on, carrying the giant rank numeral.
 *
 * Flat and shadow-free by design — no colored glow, only fill: the card's gradient gives it its
 * identity, the plinth's flat tint and top edge give the block below it its own. Colors
 * intentionally reuse the exact tokens already used elsewhere for the same rank (the position
 * badge, the champion name) so the podium reads as one system rather than introducing a parallel
 * palette.
 */
export interface RankingPodiumTier {
  /**
   * Background gradient applied to the tier's card.
   */
  readonly cardGradientClass: string;

  /**
   * Height of the plinth block the card stands on, tallest for 1st place — the podium's staircase,
   * built from this block rather than a `translate` hack on the card above it.
   */
  readonly plinthHeightClass: string;

  /**
   * Flat background fill of the plinth block.
   */
  readonly plinthFillClass: string;

  /**
   * Top-edge highlight of the plinth block, reading as its lit front bevel.
   */
  readonly plinthEdgeClass: string;

  /**
   * Text color of the giant rank numeral carved into the plinth.
   */
  readonly numeralColorClass: string;
}

/**
 * Tier treatment for 1st, 2nd and 3rd place, indexed by `position - 1`.
 */
const RANKING_PODIUM_TIERS: readonly RankingPodiumTier[] = [
  {
    cardGradientClass: 'bg-gradient-to-b from-accent-gold/20 via-surface-900/60 to-surface-950/80',
    plinthHeightClass: 'h-28 sm:h-32',
    plinthFillClass: 'bg-accent-gold/5',
    plinthEdgeClass: 'border-t border-accent-gold/30',
    numeralColorClass: 'text-accent-gold',
  },
  {
    cardGradientClass:
      'bg-gradient-to-b from-text-secondary/10 via-surface-900/60 to-surface-950/80',
    plinthHeightClass: 'h-20 sm:h-24',
    plinthFillClass: 'bg-text-secondary/5',
    plinthEdgeClass: 'border-t border-text-secondary/25',
    numeralColorClass: 'text-text-secondary',
  },
  {
    cardGradientClass:
      'bg-gradient-to-b from-podium-bronze/15 via-surface-900/60 to-surface-950/80',
    plinthHeightClass: 'h-16 sm:h-20',
    plinthFillClass: 'bg-podium-bronze/5',
    plinthEdgeClass: 'border-t border-podium-bronze/25',
    numeralColorClass: 'text-podium-bronze',
  },
];

/**
 * Fallback tier used if a podium row ever carries a position outside 1-3, so the template never
 * indexes out of bounds.
 */
const DEFAULT_TIER = RANKING_PODIUM_TIERS[2];

/**
 * Resolves the visual tier for a podium row's 1-based position.
 *
 * @param position - The row's ranking position.
 * @returns The tier treatment to render, defaulting to the bronze tier for any unexpected value.
 */
export function resolveRankingPodiumTier(position: number): RankingPodiumTier {
  return RANKING_PODIUM_TIERS[position - 1] ?? DEFAULT_TIER;
}

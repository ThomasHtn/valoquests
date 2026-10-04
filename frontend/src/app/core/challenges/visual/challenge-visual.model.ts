/**
 * Challenge icon, matched by a `@switch` since each Lucide icon is its own directive.
 */
export type ChallengeIcon =
  | 'skull'
  | 'crosshair'
  | 'trophy'
  | 'users'
  | 'star'
  | 'swords'
  | 'activity'
  | 'shield'
  | 'trending-up'
  | 'calendar'
  | 'target';

/**
 * Tier numeral from easiest (`I`) to hardest (`V`), or `D` for the daily challenge.
 */
export type ChallengeTier = 'I' | 'II' | 'III' | 'IV' | 'V' | 'D';

/**
 * Challenge icon and colours; classes are full literals so Tailwind's scanner finds them.
 */
export interface ChallengeVisual {
  /**
   * Metric icon.
   */
  readonly icon: ChallengeIcon;

  /**
   * Tier numeral in the hex badge, so the tier is not conveyed by colour alone.
   */
  readonly tier: ChallengeTier;

  /**
   * Icon Tailwind class.
   */
  readonly iconClass: string;

  /**
   * Badge Tailwind class.
   */
  readonly badgeClass: string;

  /**
   * Progress bar Tailwind class.
   */
  readonly barClass: string;

  /**
   * Bare tier accent, set as one `--tier` custom property to light a whole block.
   */
  readonly tierColor: string;

  /**
   * Card border and gradient origin, paired with the card's own `bg-linear-*`.
   */
  readonly panelClass: string;
}

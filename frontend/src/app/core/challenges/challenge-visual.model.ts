/**
 * Icon shown for a challenge, matched against a `@switch` in {@link ChallengeIconView} since each
 * Lucide icon is its own standalone directive.
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
 * Rank of a challenge's difficulty tier, written as a roman numeral from easiest (`I`) to hardest
 * (`V`), the way the design labels the five weekly slots.
 */
export type ChallengeTier = 'I' | 'II' | 'III' | 'IV' | 'V';

/**
 * Visual treatment applied to a challenge, shared by the weekly challenges card and the weekly
 * ranking table so both widgets render the same icon and color for a given challenge.
 *
 * Classes are pre-built as full literal strings (rather than composed from an accent name at
 * render time) so Tailwind's build-time class scanner can find them in this file.
 */
export interface ChallengeVisual {
  readonly icon: ChallengeIcon;

  /**
   * Difficulty rank shown inside the hex badge, so the tier is not conveyed by color alone.
   */
  readonly tier: ChallengeTier;
  readonly iconClass: string;
  readonly badgeClass: string;
  readonly barClass: string;

  /**
   * The tier's accent as a bare color, for the component stylesheets that light a whole block from
   * it through one custom property (`--tier` on the week board's cards) rather than through a class
   * per element. A class run cannot do that: the mark's fill, the mark's own inner text, the
   * caption and the squad slugs are four different properties on four elements, and Tailwind has no
   * class for "this element's accent".
   */
  readonly tierColor: string;

  /**
   * Border and gradient origin of the challenge card, tinting the whole panel with the tier's
   * accent. Paired with the card's own `bg-linear-*` direction and end color.
   */
  readonly panelClass: string;
}

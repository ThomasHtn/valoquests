/**
 * Tier numeral from easiest (`I`) to hardest (`V`), or `D` for the daily challenge.
 */
export type ChallengeTier = 'I' | 'II' | 'III' | 'IV' | 'V' | 'D';

/**
 * Challenge tier badge and colour.
 */
export interface ChallengeVisual {
  /**
   * Tier numeral in the hex badge, so the tier is not conveyed by colour alone.
   */
  readonly tier: ChallengeTier;

  /**
   * Tier accent as a CSS colour (`var(--color-accent-green)`), bound as `--tone`.
   */
  readonly tierColor: string;
}

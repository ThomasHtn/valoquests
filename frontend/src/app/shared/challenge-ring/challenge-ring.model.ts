/**
 * A single player's progress toward one challenge, the shape {@link ChallengeRing} needs to draw
 * itself — deliberately narrower than `RankingCell` (`pages/leaderboard/leaderboard.model.ts`), so
 * this shared component does not depend on that page's own model. `RankingCell` structurally
 * satisfies it as-is.
 */
export interface ChallengeRingCell {
  /**
   * Translated metric category.
   */
  readonly categoryLabel: string;

  /**
   * Progress so far, formatted.
   */
  readonly currentValueLabel: string;

  /**
   * Progress so far, abbreviated for the ring.
   */
  readonly compactValueLabel: string;

  /**
   * Target, formatted, or `null` when open-ended.
   */
  readonly targetValueLabel: string | null;

  /**
   * Share of the target reached, in percent.
   */
  readonly completionPercentage: number;

  /**
   * Whether the challenge is validated.
   */
  readonly completed: boolean;

  /**
   * Icon and badge classes of the challenge.
   */
  readonly visual: {
    /**
     * Tailwind class of the icon.
     */
    readonly iconClass: string;

    /**
     * Tailwind class of the badge.
     */
    readonly badgeClass: string;
  };
}

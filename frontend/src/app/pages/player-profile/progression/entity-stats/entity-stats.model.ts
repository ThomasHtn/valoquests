/**
 * One row of the table: a map or an agent, and how the player does on it.
 */
export interface EntityStatsRow {
  /**
   * Display name, also the row's tracking key.
   */
  readonly name: string;

  /**
   * Resolved portrait, or `null` when the application ships no image for it.
   */
  readonly imageUrl: string | null;

  /**
   * Fallback letter shown when there is no portrait.
   */
  readonly monogram: string;

  /**
   * Matches played on it.
   */
  readonly matchesPlayed: number;

  /**
   * Share of those matches won, as a percentage.
   */
  readonly winRate: number;

  /**
   * Average combat score across those matches.
   */
  readonly acs: number;
}

/**
 * Table row: a map or an agent and how the player does on it.
 */
export interface EntityStatsRow {
  /**
   * Display name, also the tracking key.
   */
  readonly name: string;

  /**
   * Portrait URL, `null` when the app ships no image for it.
   */
  readonly imageUrl: string | null;

  /**
   * Fallback letter without a portrait.
   */
  readonly monogram: string;

  /**
   * Matches played on it.
   */
  readonly matchesPlayed: number;

  /**
   * Win rate in percent.
   */
  readonly winRate: number;

  /**
   * Average combat score.
   */
  readonly acs: number;
}

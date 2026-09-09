/**
 * Season available for filtering a player's match history, as exposed by `GET /api/seasons`.
 *
 * Mirrors the backend `SeasonResponse`.
 */
export interface Season {
  /**
   * Internal identifier of the season.
   */
  readonly id: number;

  /**
   * Raw season code, e.g. `e9a2`.
   */
  readonly name: string;

  /**
   * Whether this is the season in progress.
   */
  readonly active: boolean;
}

/**
 * Season available for filtering a player's match history, as exposed by `GET /api/seasons`.
 *
 * Mirrors the backend `SeasonResponse`.
 */
export interface Season {
  readonly id: number;
  readonly name: string;
  readonly active: boolean;
}

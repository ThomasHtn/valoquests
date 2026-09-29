import { Match } from './match.model';

/**
 * One of the campaign roster's matches of the day, as exposed by `GET /api/matches`.
 *
 * Mirrors the backend `SquadMatchResponse`.
 */
export interface SquadMatch {
  /**
   * Internal identifier of the player who played the match.
   */
  readonly playerId: number;

  /**
   * Name shown across the application.
   */
  readonly displayName: string;

  /**
   * Agent name backing the player's bundled avatar, or `null` when unset.
   */
  readonly portrait: string | null;

  /**
   * The player's match, as the player's own history exposes it.
   */
  readonly match: Match;
}

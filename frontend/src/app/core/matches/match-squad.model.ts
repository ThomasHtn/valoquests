import { Match } from './match.model';

/**
 * One of the roster's matches of the day. Mirrors the backend `SquadMatchResponse`.
 */
export interface SquadMatch {
  /**
   * Identifier of the player who played the match.
   */
  readonly playerId: number;

  /**
   * Display name.
   */
  readonly displayName: string;

  /**
   * Agent name backing the bundled avatar, `null` when unset.
   */
  readonly portrait: string | null;

  /**
   * The match, as the player's own history exposes it.
   */
  readonly match: Match;
}

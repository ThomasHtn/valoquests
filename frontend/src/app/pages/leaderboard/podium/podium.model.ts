import { BoardRow } from '../leaderboard.model';

/**
 * Podium plinth, shared by every operator tied on its place.
 */
export interface PodiumPlace {
  /**
   * Place, 1 to 3.
   */
  readonly position: number;

  /**
   * Operators on it, several on a tie.
   */
  readonly rows: readonly BoardRow[];
}

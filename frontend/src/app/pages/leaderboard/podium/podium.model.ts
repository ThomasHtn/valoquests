import { BoardRow } from '../leaderboard.model';

/**
 * One plinth of the podium, shared by every operator tied on its place.
 */
export interface PodiumPlace {
  /**
   * The place the plinth stands for, 1 to 3.
   */
  readonly position: number;

  /**
   * Operators standing on it, more than one on a tie.
   */
  readonly rows: readonly BoardRow[];
}

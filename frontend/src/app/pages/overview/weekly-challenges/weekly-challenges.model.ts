import { ChallengeVisual } from '@core/challenges/challenge-visual.model';

/**
 * Single row of the weekly challenges list: a challenge paired with its resolved visual treatment.
 */
export interface ChallengeRow {
  readonly id: number;
  readonly name: string;
  readonly description: string;
  readonly completedPlayers: number;
  readonly totalPlayers: number;
  readonly completionPercentage: number;
  readonly points: number;
  readonly visual: ChallengeVisual;
}

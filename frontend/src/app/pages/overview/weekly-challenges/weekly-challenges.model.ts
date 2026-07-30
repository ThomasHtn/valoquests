import { ChallengeDifficulty } from '@core/challenges/challenge.model';
import { ChallengeVisual } from '@core/challenges/challenge-visual.model';

/**
 * Single row of the weekly challenges list: a challenge paired with its resolved visual treatment.
 */
export interface ChallengeRow {
  readonly id: number;
  readonly name: string;
  readonly description: string;

  /**
   * Difficulty tier, rendered as text beside the challenge name so the tier is not conveyed by
   * {@link visual}'s color alone (WCAG 1.4.1).
   */
  readonly difficulty: ChallengeDifficulty;
  readonly completedPlayers: number;
  readonly totalPlayers: number;
  readonly completionPercentage: number;
  readonly points: number;
  readonly visual: ChallengeVisual;
}

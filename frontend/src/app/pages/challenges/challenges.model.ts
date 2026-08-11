import { ChallengeDifficulty } from '@core/challenges/challenge.model';
import { ChallengeVisual } from '@core/challenges/challenge-visual.model';

/**
 * Single card of the weekly quest board: a challenge paired with its resolved visual treatment.
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

  /**
   * Damage the challenge deals to the week's boss once completed, already grouped for the active
   * language (e.g. `9 000`).
   */
  readonly damage: string;
  readonly visual: ChallengeVisual;
}

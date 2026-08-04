import { ChallengeDifficulty } from '@core/challenges/challenge.model';
import { ChallengeVisual } from '@core/challenges/challenge-visual.model';

/**
 * Single player shown in a challenge's avatar stack.
 */
export interface ChallengeContributor {
  readonly playerId: number;
  readonly displayName: string;
  readonly avatarUrl: string | null;

  /**
   * Whether the player has completed this specific challenge.
   */
  readonly contributed: boolean;
}

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
  readonly damage: number;
  readonly visual: ChallengeVisual;

  /**
   * Every tracked player, paired with whether they have completed this challenge, shown as an
   * avatar stack instead of a bare completion fraction.
   */
  readonly contributors: readonly ChallengeContributor[];
}

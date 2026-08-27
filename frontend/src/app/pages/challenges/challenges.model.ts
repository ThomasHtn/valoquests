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
   * language (e.g. `9 000`). The base amount, which never moves during the week.
   */
  readonly damage: string;

  /**
   * Materials one player banks for the colony by clearing it, already grouped.
   *
   * The other half of what a challenge is worth, and the half nothing used to state: the damage
   * moves the weekly ranking and the boss fight, the materials move the town's tiers. Without it the
   * campaign page asks for materials and no screen says where they come from.
   */
  readonly materials: string;

  /**
   * Squad multiplier currently applied to {@link damage}, as `"×1,2"`, or `null` while no bonus is
   * earned yet — a lone completer multiplies by one, and showing that reads as a penalty.
   */
  readonly squadMultiplier: string | null;
  readonly visual: ChallengeVisual;
}

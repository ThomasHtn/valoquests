import { CHALLENGE_DIFFICULTY_COLORS, DAILY_CHALLENGE_VISUAL } from './challenge-visual.constants';
import { ChallengeVisual } from './challenge-visual.model';
import { ChallengeDifficulty } from '../challenge.model';

/**
 * Tier treatment of a difficulty, `null` for the daily challenge.
 */
export function resolveDifficultyVisual(difficulty: ChallengeDifficulty | null): ChallengeVisual {
  return difficulty === null ? DAILY_CHALLENGE_VISUAL : CHALLENGE_DIFFICULTY_COLORS[difficulty];
}

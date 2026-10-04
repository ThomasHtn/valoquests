import {
  CHALLENGE_DIFFICULTY_COLORS,
  CHALLENGE_METRIC_ICONS,
  DAILY_CHALLENGE_VISUAL,
  DEFAULT_CHALLENGE_ICON,
} from './challenge-visual.constants';
import { ChallengeVisual } from './challenge-visual.model';
import { ChallengeDifficulty } from '../challenge.model';

/**
 * Tier treatment of a difficulty without icon, `null` for the daily challenge.
 */
export function resolveDifficultyVisual(
  difficulty: ChallengeDifficulty | null,
): Omit<ChallengeVisual, 'icon'> {
  return difficulty === null ? DAILY_CHALLENGE_VISUAL : CHALLENGE_DIFFICULTY_COLORS[difficulty];
}

/**
 * Challenge treatment: icon from the first metric of a composite, colour from the tier.
 */
export function resolveChallengeVisual(
  metric: string,
  difficulty: ChallengeDifficulty | null,
): ChallengeVisual {
  const [primaryMetric] = metric.split(' + ');
  return {
    icon: CHALLENGE_METRIC_ICONS[primaryMetric] ?? DEFAULT_CHALLENGE_ICON,
    ...resolveDifficultyVisual(difficulty),
  };
}

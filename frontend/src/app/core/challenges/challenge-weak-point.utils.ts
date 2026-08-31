import { Language } from '@core/i18n/translation.model';
import { ChallengeProgress } from './challenge.model';
import { formatDamage } from './challenge-format.utils';
import { resolveChallengeVisual } from './challenge-visual.utils';
import { ChallengeWeakPoint } from './challenge-weak-point.model';

/**
 * Reduces the active week's challenges to the boss's weak points: a tier mark and a damage amount,
 * in the order they were drawn.
 *
 * @param challenges - The active week's challenges, with their collective progress.
 * @param language - Active language, used to group each damage amount.
 * @returns The corresponding weak points, in the same order.
 */
export function resolveChallengeWeakPoints(
  challenges: readonly ChallengeProgress[],
  language: Language,
): readonly ChallengeWeakPoint[] {
  return challenges.map((challenge) => {
    const visual = resolveChallengeVisual(challenge.metric, challenge.difficulty);

    return {
      id: challenge.id,
      tier: visual.tier,
      iconClass: visual.iconClass,
      barClass: visual.barClass,
      tierColor: visual.tierColor,
      difficulty: challenge.difficulty,
      damageLabel: formatDamage(challenge.damage, language),
    };
  });
}

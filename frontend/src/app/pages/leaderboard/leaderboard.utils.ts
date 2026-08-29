import { formatDamage } from '@core/challenges/challenge-format.utils';
import { RankingChallengeProgress } from '@core/ranking/ranking.model';

/**
 * Computes how close a player's progress is to a challenge's target, as a percentage clamped
 * between 0 and 100.
 *
 * @param progress - The player's progress toward the challenge, or `undefined` if not started.
 * @returns The completion percentage, or `100` when the progress is already marked completed
 * without a numeric target (composite challenges).
 */
export function computeCompletionPercentage(
  progress: RankingChallengeProgress | undefined,
): number {
  if (!progress) {
    return 0;
  }
  if (!progress.targetValue) {
    return progress.completed ? 100 : 0;
  }
  return Math.min(100, Math.max(0, (progress.currentValue / progress.targetValue) * 100));
}

/**
 * Formats a metric value for display: whole numbers are grouped, other values (e.g. a K/D ratio)
 * are rounded to one decimal in the reader's own notation.
 *
 * Grouped for the same reason the damage amounts are: a challenge asking for 80 000 points of
 * damage was printed `80000` right beside a total printed `1 650`, and the two read as if they came
 * from different screens.
 *
 * @param value - The raw metric value.
 * @param language - The app language whose grouping and decimal separators to use.
 * @returns The formatted value as a string.
 */
export function formatMetricValue(value: number, language: 'fr' | 'en'): string {
  return Number.isInteger(value)
    ? formatDamage(value, language)
    : new Intl.NumberFormat(language === 'fr' ? 'fr-FR' : 'en-US', {
        minimumFractionDigits: 1,
        maximumFractionDigits: 1,
      }).format(value);
}

/**
 * Builds the current value label for a challenge progress cell, e.g. `"42"`.
 *
 * @param progress - The player's progress toward the challenge, or `undefined` if not started.
 * @param language - The app language whose separators to use.
 * @returns The pre-formatted current value label.
 */
export function buildCurrentValueLabel(
  progress: RankingChallengeProgress | undefined,
  language: 'fr' | 'en',
): string {
  return formatMetricValue(progress?.currentValue ?? 0, language);
}

/**
 * Builds the target value label for a challenge progress cell, e.g. `"100"`.
 *
 * @param progress - The player's progress toward the challenge, or `undefined` if not started.
 * @param language - The app language whose separators to use.
 * @returns The pre-formatted target value label, or `null` for composite challenges with no
 * stored target.
 */
export function buildTargetValueLabel(
  progress: RankingChallengeProgress | undefined,
  language: 'fr' | 'en',
): string | null {
  return progress?.targetValue ? formatMetricValue(progress.targetValue, language) : null;
}

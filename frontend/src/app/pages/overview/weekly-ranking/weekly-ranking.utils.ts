import { RankingChallengeProgress } from '../../../core/ranking/ranking.model';

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
 * Formats a metric value for display: whole numbers are shown as-is, other values (e.g. a K/D
 * ratio) are rounded to one decimal.
 *
 * @param value - The raw metric value.
 * @returns The formatted value as a string.
 */
export function formatMetricValue(value: number): string {
  return Number.isInteger(value) ? `${value}` : value.toFixed(1);
}

/**
 * Builds the display label for a challenge progress cell, e.g. `"42/100"`, or just the current
 * value when the challenge has no stored target (composite challenges).
 *
 * @param progress - The player's progress toward the challenge, or `undefined` if not started.
 * @returns The pre-formatted progress label.
 */
export function buildValueLabel(progress: RankingChallengeProgress | undefined): string {
  if (!progress) {
    return formatMetricValue(0);
  }
  return progress.targetValue
    ? `${formatMetricValue(progress.currentValue)}/${formatMetricValue(progress.targetValue)}`
    : formatMetricValue(progress.currentValue);
}

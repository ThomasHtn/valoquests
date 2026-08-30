import { formatDamage } from '@core/challenges/challenge-format.utils';
import { ChallengeProgress } from '@core/challenges/challenge.model';
import {
  resolveChallengeMetricLabel,
  resolveChallengeVisual,
} from '@core/challenges/challenge-visual.utils';
import { RankingChallengeProgress } from '@core/ranking/ranking.model';
import { RankingCell, RankingColumn } from './leaderboard.model';

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
 * Builds the abbreviated form of the same value, e.g. `"128 k"` — what a challenge ring falls back
 * to when the grouped figure is wider than the ring can hold.
 *
 * A second label rather than a replacement: the exact value still has to reach assistive
 * technology, and every layout wider than a 44px ring keeps showing it in full.
 *
 * @param progress - The player's progress toward the challenge, or `undefined` if not started.
 * @param language - The app language whose separators and unit suffix to use.
 * @returns The abbreviated current value.
 */
export function buildCompactValueLabel(
  progress: RankingChallengeProgress | undefined,
  language: 'fr' | 'en',
): string {
  // Compact notation's own rounding, with no `maximumFractionDigits` of ours over it: it already
  // spends a decimal only where one carries information (`1,3 M`, but `128 k`), and pinning the
  // digits either forces one onto every value — `128,5 k`, wider than the figure it replaces — or
  // strips the one place a single-digit magnitude has left.
  return new Intl.NumberFormat(language === 'fr' ? 'fr-FR' : 'en-US', {
    notation: 'compact',
  }).format(progress?.currentValue ?? 0);
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

/**
 * Resolves the active week's challenges into ranking table columns — the week's own draw, paired
 * with the color treatment the weekly challenges card already uses for the same challenges.
 *
 * Shared by `Leaderboard` (the full matrix) and `PlayerProfile` (one player's own five rings),
 * so a challenge reads the same tier badge and target everywhere it appears this week.
 *
 * @param challenges - The active week's drawn challenges, or an empty list on a closed week.
 * @param translate - Resolves an i18n key, used for each challenge's metric category label.
 * @param language - The app language whose separators format each target value with.
 * @returns One column per challenge, in the order the backend drew them.
 */
export function resolveRankingColumns(
  challenges: readonly ChallengeProgress[],
  translate: (key: string) => string,
  language: 'fr' | 'en',
): readonly RankingColumn[] {
  return challenges.map((challenge) => ({
    challengeId: challenge.id,
    name: challenge.name,
    categoryLabel: resolveChallengeMetricLabel(challenge.metric, translate),
    targetLabel: challenge.targetValue ? formatMetricValue(challenge.targetValue, language) : null,
    tooltip: `${challenge.name} — ${challenge.description}`,
    visual: resolveChallengeVisual(challenge.metric, challenge.difficulty),
  }));
}

/**
 * Aligns one player's per-challenge progress with the week's columns, one cell per column — the
 * same pairing `Leaderboard` builds for every row of the matrix.
 *
 * @param columns - The active week's columns, as resolved by {@link resolveRankingColumns}.
 * @param progress - The player's own progress entries, in no particular order.
 * @param language - The app language whose separators format each cell's values with.
 * @returns One cell per column, aligned by challenge id.
 */
export function resolveRankingCells(
  columns: readonly RankingColumn[],
  progress: readonly RankingChallengeProgress[],
  language: 'fr' | 'en',
): readonly RankingCell[] {
  return columns.map((column) => {
    const match = progress.find((candidate) => candidate.challengeId === column.challengeId);
    return {
      challengeId: column.challengeId,
      name: column.name,
      categoryLabel: column.categoryLabel,
      currentValueLabel: buildCurrentValueLabel(match, language),
      compactValueLabel: buildCompactValueLabel(match, language),
      targetValueLabel: buildTargetValueLabel(match, language),
      completionPercentage: computeCompletionPercentage(match),
      completed: match?.completed ?? false,
      visual: column.visual,
    };
  });
}

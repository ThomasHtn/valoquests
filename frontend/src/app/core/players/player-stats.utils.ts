/**
 * Text and progress-bar color applied to a statistic, kept together so a value is never rendered
 * with a label and a bar that disagree on how good it is.
 *
 * Classes are pre-built as full literal strings (rather than composed from an accent name at
 * render time) so Tailwind's build-time class scanner can find them in this file.
 */
export interface StatVisual {
  readonly textClass: string;
  readonly barClass: string;
}

/**
 * Thresholds above which a win rate or KDA is considered good enough to be highlighted in green
 * rather than gold, mirroring the challenge difficulty color language (green for favorable, gold
 * for attention).
 */
const WIN_RATE_GOOD_THRESHOLD = 50;
const KDA_GOOD_THRESHOLD = 1.3;

/**
 * Visual treatment applied to a statistic that has not been synchronized yet.
 */
const UNKNOWN_STAT_VISUAL: StatVisual = {
  textClass: 'text-text-secondary',
  barClass: 'bg-text-secondary',
};

const GOOD_STAT_VISUAL: StatVisual = {
  textClass: 'text-accent-green',
  barClass: 'bg-accent-green',
};

const AVERAGE_STAT_VISUAL: StatVisual = {
  textClass: 'text-accent-gold',
  barClass: 'bg-accent-gold',
};

/**
 * Resolves the visual treatment for a statistic compared against a "good enough" threshold.
 *
 * @param value - The statistic's value, or `null` when not yet synchronized.
 * @param goodThreshold - Value at or above which the statistic is highlighted as favorable.
 * @returns The text and bar colors to apply.
 */
function resolveStatVisual(value: number | null, goodThreshold: number): StatVisual {
  if (value === null) {
    return UNKNOWN_STAT_VISUAL;
  }
  return value >= goodThreshold ? GOOD_STAT_VISUAL : AVERAGE_STAT_VISUAL;
}

/**
 * Resolves the visual treatment for a win rate value.
 *
 * @param winRate - The player's win rate percentage, or `null` when not yet synchronized.
 * @returns The text and bar colors to apply.
 */
export function resolveWinRateVisual(winRate: number | null): StatVisual {
  return resolveStatVisual(winRate, WIN_RATE_GOOD_THRESHOLD);
}

/**
 * Resolves the visual treatment for a KDA value.
 *
 * @param kda - The player's KDA ratio, or `null` when not yet synchronized.
 * @returns The text and bar colors to apply.
 */
export function resolveKdaVisual(kda: number | null): StatVisual {
  return resolveStatVisual(kda, KDA_GOOD_THRESHOLD);
}

/**
 * Threshold above which a win rate or KDA is considered good enough to be highlighted in green
 * rather than gold, mirroring the challenge difficulty color language (green for favorable, gold
 * for attention).
 */
const WIN_RATE_GOOD_THRESHOLD = 50;
const KDA_GOOD_THRESHOLD = 1.3;

/**
 * Resolves the color class for a win rate value.
 *
 * @param winRate - The player's win rate percentage, or `null` when not yet synchronized.
 * @returns The Tailwind text/background color utility to apply.
 */
export function resolveWinRateColorClass(winRate: number | null): string {
  if (winRate === null) {
    return 'text-text-secondary';
  }
  return winRate >= WIN_RATE_GOOD_THRESHOLD ? 'text-accent-green' : 'text-accent-gold';
}

/**
 * Resolves the progress bar fill color for a win rate value, paired with
 * {@link resolveWinRateColorClass}.
 *
 * @param winRate - The player's win rate percentage, or `null` when not yet synchronized.
 * @returns The Tailwind background color utility to apply to the bar's fill.
 */
export function resolveWinRateBarClass(winRate: number | null): string {
  if (winRate === null) {
    return 'bg-text-secondary';
  }
  return winRate >= WIN_RATE_GOOD_THRESHOLD ? 'bg-accent-green' : 'bg-accent-gold';
}

/**
 * Resolves the color class for a KDA value.
 *
 * @param kda - The player's KDA ratio, or `null` when not yet synchronized.
 * @returns The Tailwind text color utility to apply.
 */
export function resolveKdaColorClass(kda: number | null): string {
  if (kda === null) {
    return 'text-text-secondary';
  }
  return kda >= KDA_GOOD_THRESHOLD ? 'text-accent-green' : 'text-accent-gold';
}

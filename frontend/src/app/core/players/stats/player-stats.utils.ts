import {
  AVERAGE_STAT_VISUAL,
  GOOD_STAT_VISUAL,
  KD_GOOD_THRESHOLD,
  KDA_GOOD_THRESHOLD,
  UNKNOWN_STAT_VISUAL,
  WIN_RATE_GOOD_THRESHOLD,
} from './player-stats.constants';
import { StatVisual } from './player-stats.model';

/**
 * Colours of a statistic against its good threshold, muted when `null`.
 */
function resolveStatVisual(value: number | null, goodThreshold: number): StatVisual {
  if (value === null) {
    return UNKNOWN_STAT_VISUAL;
  }
  return value >= goodThreshold ? GOOD_STAT_VISUAL : AVERAGE_STAT_VISUAL;
}

/**
 * Colours of a win rate.
 */
export function resolveWinRateVisual(winRate: number | null): StatVisual {
  return resolveStatVisual(winRate, WIN_RATE_GOOD_THRESHOLD);
}

/**
 * Colours of a KDA.
 */
export function resolveKdaVisual(kda: number | null): StatVisual {
  return resolveStatVisual(kda, KDA_GOOD_THRESHOLD);
}

/**
 * Colours of a K/D.
 */
export function resolveKdVisual(kd: number | null): StatVisual {
  return resolveStatVisual(kd, KD_GOOD_THRESHOLD);
}

/**
 * Text colour of a stat cell: `reportedClass` when reported, muted for the missing-value dash.
 */
export function resolveStatTextClass(
  value: number | null | undefined,
  reportedClass = 'text-text-primary',
): string {
  return Number.isFinite(value) ? reportedClass : 'text-text-muted';
}

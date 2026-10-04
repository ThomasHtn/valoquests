import { StatVisual } from './player-stats.model';

/**
 * Win rate, in percent, from which a figure is shown as good (green, else gold).
 */
export const WIN_RATE_GOOD_THRESHOLD = 50;

/**
 * KDA from which a figure is shown as good.
 */
export const KDA_GOOD_THRESHOLD = 1.3;

/**
 * K/D from which a figure is shown as good, the easiest K/D challenge's bar.
 */
export const KD_GOOD_THRESHOLD = 1;

/**
 * Colours of a figure not synchronized yet.
 */
export const UNKNOWN_STAT_VISUAL: StatVisual = {
  textClass: 'text-text-secondary',
  barClass: 'bg-text-secondary',
};

/**
 * Colours of a figure at or above its good threshold.
 */
export const GOOD_STAT_VISUAL: StatVisual = {
  textClass: 'text-accent-green',
  barClass: 'bg-accent-green',
};

/**
 * Colours of a figure below its good threshold.
 */
export const AVERAGE_STAT_VISUAL: StatVisual = {
  textClass: 'text-accent-gold',
  barClass: 'bg-accent-gold',
};

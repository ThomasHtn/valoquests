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
 * Colour of a figure not synchronized yet.
 */
export const UNKNOWN_STAT_VISUAL: StatVisual = {
  tone: 'var(--color-text-secondary)',
};

/**
 * Colour of a figure at or above its good threshold.
 */
export const GOOD_STAT_VISUAL: StatVisual = {
  tone: 'var(--color-accent-green)',
};

/**
 * Colour of a figure below its good threshold.
 */
export const AVERAGE_STAT_VISUAL: StatVisual = {
  tone: 'var(--color-accent-gold)',
};

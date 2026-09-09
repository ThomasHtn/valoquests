import { StatusBadgeTone } from './status-badge.model';

/**
 * Border, background and text colour of each tone.
 */
export const TONE_CLASS: Record<StatusBadgeTone, string> = {
  brand: 'border-brand-500/50 bg-brand-500/12 text-brand-400',
  neutral: 'border-surface-600 bg-surface-800 text-text-secondary',
  danger: 'border-danger/40 bg-danger/10 text-danger',
};

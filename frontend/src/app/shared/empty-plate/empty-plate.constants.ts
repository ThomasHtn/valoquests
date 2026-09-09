import { ReadoutTone } from './empty-plate.model';

/**
 * Tailwind classes of a readout's dot and value, by tone.
 */
export const READOUT_TONES: Record<ReadoutTone, { dot: string; value: string }> = {
  live: {
    dot: 'border-success bg-success shadow-[0_0_0_3px_rgb(95_184_138/20%)]',
    value: 'text-success',
  },
  todo: { dot: 'border-brand-500', value: 'text-text-primary' },
  info: { dot: 'border-text-muted bg-text-muted', value: 'text-text-primary' },
};

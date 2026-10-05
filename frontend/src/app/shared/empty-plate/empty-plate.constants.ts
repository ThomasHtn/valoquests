import { ReadoutTone } from './empty-plate.model';

/**
 * Modifier filling a readout's dot, by tone; `todo` stays a hollow ring.
 */
export const READOUT_DOT_CLASSES: Readonly<Record<ReadoutTone, string>> = {
  live: 'empty-plate__dot--live',
  todo: '',
  info: 'empty-plate__dot--info',
};

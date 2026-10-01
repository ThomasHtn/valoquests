import { KeyFigureTone } from './key-figures.model';

/**
 * Tile classes per tone: a flat tint of the colour behind a pictogram in that colour.
 */
export const KEY_FIGURE_TONE_CLASSES: Readonly<Record<KeyFigureTone, string>> = {
  brand: 'bg-brand-500/12 text-brand-500',
  good: 'bg-success/12 text-success',
  bad: 'bg-danger/12 text-danger',
  neutral: 'bg-text-primary/4 text-text-secondary',
};

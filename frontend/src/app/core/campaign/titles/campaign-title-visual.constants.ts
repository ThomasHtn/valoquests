import { TitleKey } from './campaign-title.model';
import { TitleVisual } from './campaign-title-visual.model';

/**
 * Icon and colour of each title, those of the resource it rewards.
 */
export const TITLE_VISUALS: Readonly<Record<TitleKey, TitleVisual>> = {
  CHAMPION: { icon: 'crown', tone: 'var(--color-brand-500)' },
  MECHANIC: { icon: 'wrench', tone: 'var(--color-accent-cyan)' },
  QUARTERMASTER: { icon: 'wheat', tone: 'var(--color-accent-green)' },
  REGULAR: { icon: 'flame', tone: 'var(--color-accent-purple)' },
  SCOUT: { icon: 'target', tone: 'var(--color-accent-blue)' },
};

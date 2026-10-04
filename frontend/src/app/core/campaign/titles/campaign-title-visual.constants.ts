import { TitleKey } from './campaign-title.model';
import { TitleVisual } from './campaign-title-visual.model';

/**
 * Icon and colour of each title, those of the resource it rewards.
 */
export const TITLE_VISUALS: Readonly<Record<TitleKey, TitleVisual>> = {
  CHAMPION: { icon: 'crown', colorClass: 'text-brand-500' },
  MECHANIC: { icon: 'wrench', colorClass: 'text-accent-cyan' },
  QUARTERMASTER: { icon: 'wheat', colorClass: 'text-accent-green' },
  REGULAR: { icon: 'flame', colorClass: 'text-accent-purple' },
  SCOUT: { icon: 'target', colorClass: 'text-accent-blue' },
};

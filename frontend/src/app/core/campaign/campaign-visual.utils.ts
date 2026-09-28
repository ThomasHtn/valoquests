import { TitleKey } from './campaign.model';

/**
 * Icon carried by a title, matched against a `@switch` at the call site since each Lucide icon is
 * its own standalone directive.
 */
export type TitleIcon = 'crown' | 'wrench' | 'wheat' | 'flame' | 'target';

/**
 * Visual treatment of a weekly title: the icon of what it rewards and the colour of that resource.
 *
 * Classes are full literal strings so Tailwind's build-time scanner finds them here.
 */
export interface TitleVisual {
  readonly icon: TitleIcon;
  readonly colorClass: string;
}

/**
 * One word, one colour, one icon: a title is worded by the resource it rewards, so the Mechanic
 * carries the wrench and the cyan of the components everywhere the components do.
 */
const TITLE_VISUALS: Readonly<Record<TitleKey, TitleVisual>> = {
  CHAMPION: { icon: 'crown', colorClass: 'text-brand-500' },
  MECHANIC: { icon: 'wrench', colorClass: 'text-accent-cyan' },
  QUARTERMASTER: { icon: 'wheat', colorClass: 'text-accent-green' },
  REGULAR: { icon: 'flame', colorClass: 'text-accent-purple' },
  SCOUT: { icon: 'target', colorClass: 'text-accent-blue' },
};

/**
 * Resolves the icon and colour of a title.
 *
 * @param title - The title.
 * @returns Its visual treatment.
 */
export function resolveTitleVisual(title: TitleKey): TitleVisual {
  return TITLE_VISUALS[title];
}

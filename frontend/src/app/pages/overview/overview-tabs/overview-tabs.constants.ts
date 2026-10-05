import type { LucideIcon } from '@lucide/angular';
import { LucideMap, LucideRadio, LucideSunrise, LucideUserPen } from '@lucide/angular';

import { OverviewTabKey } from './overview-tabs.model';

/**
 * Tab slide duration when the pinned tab changes, in ms.
 */
export const TAB_SLIDE_MS = 280;

/**
 * Easing of the tab slide: quick start, soft settle.
 */
export const TAB_SLIDE_EASING = 'cubic-bezier(0.2, 0.8, 0.2, 1)';

/**
 * Local storage key of the pinned default tab.
 */
export const FAVORITE_TAB_KEY = 'valoquests.overview.favoriteTab';

/**
 * Overview tabs in default bar order.
 */
export const OVERVIEW_TABS: readonly OverviewTabKey[] = [
  'challenges',
  'contributions',
  'matches',
  'campaign',
];

/**
 * Icon drawn before each tab's label.
 */
export const OVERVIEW_TAB_ICONS: Readonly<Record<OverviewTabKey, LucideIcon>> = {
  challenges: LucideSunrise,
  contributions: LucideUserPen,
  matches: LucideRadio,
  campaign: LucideMap,
};

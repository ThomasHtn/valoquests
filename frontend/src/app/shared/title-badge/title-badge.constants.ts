import type { LucideIcon } from '@lucide/angular';
import { LucideCrown, LucideFlame, LucideTarget, LucideWheat, LucideWrench } from '@lucide/angular';

import { TitleIcon } from '@core/campaign/titles/campaign-title-visual.model';

/**
 * Lucide icon of each title icon key.
 */
export const TITLE_BADGE_ICONS: Readonly<Record<TitleIcon, LucideIcon>> = {
  crown: LucideCrown,
  wrench: LucideWrench,
  wheat: LucideWheat,
  flame: LucideFlame,
  target: LucideTarget,
};

import { LucideIcon } from '@lucide/angular';

import { LedgerKey } from '../campaign-panel.model';

/**
 * One segment of the rescue bar and its legend line.
 */
export interface RescueShare {
  /**
   * Colour modifier of the segment and its swatch.
   */
  readonly tone: 'extraction' | 'challenges' | 'left-behind';

  /**
   * Translation key of the legend line.
   */
  readonly labelKey: string;

  /**
   * Width of the segment, in percent of the wounded spotted.
   */
  readonly percent: number;

  /**
   * Wounded the segment counts.
   */
  readonly count: number;
}

/**
 * One tally of the band naming what capped each settled Sunday.
 */
export interface SundayLimit {
  /**
   * Colour modifier of the tally.
   */
  readonly tone: LedgerKey | 'group';

  /**
   * Icon in the tally's hex mark.
   */
  readonly icon: LucideIcon;

  /**
   * Sundays the tally counts.
   */
  readonly count: number;

  /**
   * Translation key of the caption, pluralised on `count`.
   */
  readonly labelKey: string;
}

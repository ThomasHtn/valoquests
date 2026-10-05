import type { LucideIcon } from '@lucide/angular';

import { Gauge } from '@pages/overview/extraction-gauges/extraction-gauges.model';

/**
 * Which stock a dial reads: components carry the wounded, food shelters them.
 */
export type TourCapacityKind = 'carry' | 'shelter';

/**
 * One resource dial of the tour's capacity step.
 */
export interface TourCapacityTile {
  /**
   * Stock the dial reads, which also picks its tone.
   */
  readonly kind: TourCapacityKind;

  /**
   * Translation key of the dial's name.
   */
  readonly nameKey: string;

  /**
   * Wounded the stock allows, its fill and the stock behind it.
   */
  readonly gauge: Gauge;

  /**
   * Icon inside the dial.
   */
  readonly gaugeIcon: LucideIcon;

  /**
   * Icon of the resource, before the stock and the rescue cost.
   */
  readonly resourceIcon: LucideIcon;

  /**
   * Translation key of the resource's name, after the stock.
   */
  readonly resourceKey: string;

  /**
   * Game modes feeding the stock, as `common.gameMode` keys them.
   */
  readonly modes: readonly string[];

  /**
   * Units of the resource one rescue costs.
   */
  readonly rescueCost: number;

  /**
   * Translation key of the rescue cost's label.
   */
  readonly rescueCostKey: string;
}

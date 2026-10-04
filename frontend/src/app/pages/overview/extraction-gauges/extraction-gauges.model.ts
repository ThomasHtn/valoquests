import { ExtractionLimiter } from '@core/campaign/campaign-week.model';

/**
 * Extraction dial: a figure over the wounded spotted, and its raw stock.
 */
export interface Gauge {
  /**
   * Wounded the dial allows.
   */
  readonly value: number;

  /**
   * Fill of the dial, in [0, 1].
   */
  readonly fraction: number;

  /**
   * Units behind the dial.
   */
  readonly stock: number;
}

/**
 * The four dials: three limits, then what gets through.
 */
export interface Capacity {
  /**
   * Wounded waiting on the planet.
   */
  readonly wounded: number;

  /**
   * Wounded the components can carry.
   */
  readonly carry: Gauge;

  /**
   * Wounded the food can shelter.
   */
  readonly shelter: Gauge;

  /**
   * Breakthrough dial.
   */
  readonly breach: Gauge;

  /**
   * Wounded aboard tonight.
   */
  readonly aboard: number;

  /**
   * Aboard as a share of the wounded, in [0, 1].
   */
  readonly aboardFraction: number;

  /**
   * Wounded freed by the breakthrough.
   */
  readonly fromGuardian: number;

  /**
   * Wounded freed by challenges.
   */
  readonly fromChallenges: number;

  /**
   * Wounded left on the planet.
   */
  readonly leftBehind: number;

  /**
   * What capped the extraction: a stock, the group, or nothing.
   */
  readonly limiter: ExtractionLimiter;

  /**
   * Components one rescue costs.
   */
  readonly componentsPerRescue: number;

  /**
   * Food one rescue costs.
   */
  readonly foodPerRescue: number;

  /**
   * Hit points one percent of breakthrough costs.
   */
  readonly hitPointsPerPercent: number;
}

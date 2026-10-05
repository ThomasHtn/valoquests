import type { LucideIcon } from '@lucide/angular';
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

/**
 * Which limit a dial measures, also its tone.
 */
export type LimitDialKey = 'carry' | 'shelter' | 'breach';

/**
 * One of the three limiting dials, every text already translated.
 */
export interface LimitDial {
  /**
   * Which limit, also the dial's tone.
   */
  readonly key: LimitDialKey;

  /**
   * Dial heading.
   */
  readonly name: string;

  /**
   * Accessible name of the info button.
   */
  readonly infoLabel: string;

  /**
   * Info button text explaining how the dial is worked out.
   */
  readonly tooltip: string;

  /**
   * Accessible reading of the dial.
   */
  readonly ariaLabel: string;

  /**
   * Fill of the ring, in [0, 1].
   */
  readonly fraction: number;

  /**
   * Figure counted up in the ring's centre.
   */
  readonly value: number;

  /**
   * Text after the figure (` %` for the breakthrough), empty otherwise.
   */
  readonly unit: string;

  /**
   * Icon above the figure.
   */
  readonly icon: LucideIcon;

  /**
   * Icon of the resource behind the dial, on its stock and rate lines.
   */
  readonly resourceIcon: LucideIcon;

  /**
   * Raw stock, formatted.
   */
  readonly stock: string;

  /**
   * Caption after the stock.
   */
  readonly stockLabel: string;

  /**
   * Game modes feeding the dial, as tags.
   */
  readonly modes: readonly string[];

  /**
   * Cost of one unit of the dial, formatted.
   */
  readonly rate: string;

  /**
   * Caption after the rate.
   */
  readonly rateLabel: string;
}

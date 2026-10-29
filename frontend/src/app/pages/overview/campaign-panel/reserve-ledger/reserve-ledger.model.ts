import { PlanetState } from '../campaign-panel.model';

/**
 * One column head of the ledger: a planet.
 */
export interface LedgerColumn {
  /**
   * Week index, one-based.
   */
  readonly index: number;

  /**
   * Name of the planet.
   */
  readonly name: string;

  /**
   * Week state.
   */
  readonly state: PlanetState;
}

/**
 * One week of one resource in the ledger.
 */
export interface LedgerCell {
  /**
   * Week index, one-based.
   */
  readonly index: number;

  /**
   * Name of the planet.
   */
  readonly planetName: string;

  /**
   * Whether the week is settled, in progress, or ahead.
   */
  readonly kind: 'settled' | 'now' | 'ahead';

  /**
   * Units gained during the week.
   */
  readonly got: number;

  /**
   * Units spent on the Sunday extraction.
   */
  readonly spent: number;

  /**
   * Stock carried into the next week.
   */
  readonly carry: number;

  /**
   * Units carried in from the previous week.
   */
  readonly carriedIn: number;

  /**
   * Stock at the end of the week's last replayed day.
   */
  readonly stock: number;

  /**
   * Rescues the week’s gain pays for.
   */
  readonly rescues: number;

  /**
   * Rescues the stock pays for.
   */
  readonly stockRescues: number;

  /**
   * Gained units as a share of the tallest bar, in [0, 1].
   */
  readonly gotShare: number;

  /**
   * Spent units as a share of the tallest bar.
   */
  readonly spentShare: number;

  /**
   * Carried units as a share of the tallest bar.
   */
  readonly carryShare: number;
}

/**
 * One row of the ledger: a resource across the ten weeks.
 */
export interface LedgerRow {
  /**
   * Resource of the row.
   */
  readonly key: 'food' | 'components';

  /**
   * Units gained over the campaign.
   */
  readonly gained: number;

  /**
   * Units spent over the campaign.
   */
  readonly spent: number;

  /**
   * One cell per planet.
   */
  readonly cells: readonly LedgerCell[];
}

/**
 * Resource a ledger row tracks.
 */
export type LedgerKey = LedgerRow['key'];

import { ConsistencyMatch } from '@core/players/progression/player-progression.model';

/**
 * Where a match falls against its season's floor and ceiling.
 */
export type ConsistencyZone = 'below' | 'inside' | 'above';

/**
 * How a season's spread compares with the previous one: narrower, wider, or about the same.
 */
export type ConsistencyTrend = 'tighter' | 'looser' | 'steady';

/**
 * Combat-score axis of the chart, with the grid of columns the dots stack in.
 */
export interface ConsistencyAxis {
  /**
   * Left end of the axis, a round combat score.
   */
  readonly min: number;

  /**
   * Right end of the axis, a round combat score.
   */
  readonly max: number;

  /**
   * Left edge of the first column of dots.
   */
  readonly binOrigin: number;

  /**
   * Number of columns of dots.
   */
  readonly binCount: number;
}

/**
 * One match drawn as a dot, stacked in its combat-score column.
 */
export interface ConsistencyDot {
  /**
   * Centre of the dot's column, in combat score.
   */
  readonly x: number;

  /**
   * Height of the dot in its stack, half a level above its floor.
   */
  readonly y: number;

  /**
   * Where the match falls against the season's floor and ceiling.
   */
  readonly zone: ConsistencyZone;

  /**
   * The match itself.
   */
  readonly match: ConsistencyMatch;
}

/**
 * Content of the tooltip shown over one match.
 */
export interface ConsistencyTooltip {
  /**
   * Combat score, rounded.
   */
  readonly acs: number;

  /**
   * Outcome and score, e.g. `Victoire 13-8`.
   */
  readonly result: string;

  /**
   * Tailwind classes of the outcome chip.
   */
  readonly resultClass: string;

  /**
   * Where the match falls against the floor and ceiling.
   */
  readonly zone: ConsistencyZone;

  /**
   * That position, spelled out.
   */
  readonly zoneLabel: string;

  /**
   * Name of the map.
   */
  readonly mapName: string;

  /**
   * Agent played.
   */
  readonly agentName: string;

  /**
   * Day the match was played, formatted.
   */
  readonly date: string;
}

/**
 * Captions of the floor and ceiling rules drawn on the chart.
 */
export interface ConsistencyBandLabels {
  /**
   * Caption of the floor rule.
   */
  readonly floor: string;

  /**
   * Caption of the ceiling rule.
   */
  readonly ceiling: string;
}

/**
 * One of the ten frieze weeks.
 */
export interface FriezeWeek {
  /**
   * Week index, one-based.
   */
  readonly index: number;

  /**
   * Two-digit week index.
   */
  readonly label: string;

  /**
   * Name of the planet.
   */
  readonly name: string;

  /**
   * Public path of the planet's drawing.
   */
  readonly art: string;

  /**
   * Won, lost, in progress, or ahead.
   */
  readonly state: 'won' | 'lost' | 'now' | 'ahead';

  /**
   * Whether Sunday settled the week, which opens its mission report.
   */
  readonly settled: boolean;

  /**
   * Share of the guardian's hit points left, in [0, 1], drawn by the planet's ring.
   */
  readonly standing: number;

  /**
   * Abbreviated guardian level, sharing a line with the status.
   */
  readonly level: string;

  /**
   * Short status shown beside the level.
   */
  readonly status: string;

  /**
   * Screen reader label: full guardian level, then the week's outcome.
   */
  readonly title: string;
}

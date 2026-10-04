/**
 * Pictogram drawn beside a key figure that has no rank badge.
 */
export type KeyFigureIcon =
  | 'trendUp'
  | 'trendDown'
  | 'flat'
  | 'floor'
  | 'median'
  | 'ceiling'
  | 'tighter'
  | 'looser'
  | 'matches';

/**
 * Pictogram tone: the section's amber, a gain, a loss, or neutral.
 */
export type KeyFigureTone = 'brand' | 'good' | 'bad' | 'neutral';

/**
 * One figure of the strip closing a progression chart.
 */
export interface KeyFigure {
  /**
   * Short name of the figure.
   */
  readonly caption: string;

  /**
   * The figure itself, formatted.
   */
  readonly value: string;

  /**
   * One line qualifying the figure.
   */
  readonly detail: string;

  /**
   * Tailwind text colour of the value, when it carries one (a rank's colour).
   */
  readonly valueClass?: string;

  /**
   * Rank badge drawn in place of a pictogram, when the figure is a rank.
   */
  readonly rankIconUrl?: string | null;

  /**
   * Pictogram drawn when there is no rank badge.
   */
  readonly icon?: KeyFigureIcon;

  /**
   * Colour of the pictogram's tile.
   */
  readonly tone?: KeyFigureTone;
}

/**
 * Point aimed at, in the planet drawing's own coordinates, and the report row it is wired to.
 */
export interface Mark {
  /**
   * X in the drawing’s viewBox.
   */
  readonly vx: number;

  /**
   * Y in the drawing’s viewBox.
   */
  readonly vy: number;

  /**
   * Data attribute of the report row the wire lands on.
   */
  readonly card: string;

  /**
   * Colour of the wire.
   */
  readonly tone: string;
}

/**
 * One confetti piece thrown by a landing check.
 */
export interface ConfettiPiece {
  /**
   * Direction in degrees, clockwise from the right.
   */
  readonly angle: number;

  /**
   * Travel distance in rem.
   */
  readonly distance: number;

  /**
   * Self-rotation along the way, in degrees.
   */
  readonly spin: number;

  /**
   * Strip rather than square fleck.
   */
  readonly strip: boolean;

  /**
   * Pale variant of the row's tone.
   */
  readonly pale: boolean;
}

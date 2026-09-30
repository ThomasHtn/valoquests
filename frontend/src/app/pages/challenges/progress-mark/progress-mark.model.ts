/**
 * One piece of the confetti a check throws when it lands.
 */
export interface ConfettiPiece {
  /**
   * Direction the piece flies in, in degrees clockwise from the right.
   */
  readonly angle: number;

  /**
   * Distance it travels from the check, in rem.
   */
  readonly distance: number;

  /**
   * Turn it makes on itself along the way, in degrees.
   */
  readonly spin: number;

  /**
   * Whether it is a strip rather than a square fleck.
   */
  readonly strip: boolean;

  /**
   * Whether it takes the pale variant of the row's tone.
   */
  readonly pale: boolean;
}

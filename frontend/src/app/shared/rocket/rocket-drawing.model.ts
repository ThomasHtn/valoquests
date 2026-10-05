/**
 * One rocket stage; each defeated guardian adds a real part, not a scale-up.
 */
export interface ShipStage {
  /**
   * Half-width of the hull, the unit most other parts scale from.
   */
  readonly w: number;

  /**
   * Height of the hull.
   */
  readonly h: number;

  /**
   * Fins drawn when non-zero; their span follows the hull width.
   */
  readonly fins: number;

  /**
   * Booster height, `0` for none.
   */
  readonly boost: number;

  /**
   * Shape of the nose.
   */
  readonly nose: 'none' | 'dome' | 'cone' | 'capsule';

  /**
   * Engine bells: `1` large one, `3` smaller ones.
   */
  readonly eng: number;

  /**
   * Service gantry: `0` none, `1` partial, `2` complete with its jib.
   */
  readonly gantry: number;

  /**
   * Porthole pairs, `0` for none.
   */
  readonly ports: number;

  /**
   * Livery bands drawn when non-zero.
   */
  readonly bands: number;
}

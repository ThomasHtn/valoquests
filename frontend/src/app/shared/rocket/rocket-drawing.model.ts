/**
 * One stage of the rocket: half-width and height of the hull, fins, booster height, nose shape,
 * engines, gantry, portholes and marking bands.
 *
 * Ten states, and each guardian defeated adds a real part: the rocket of state ten is not the one
 * of state one scaled up.
 */
export interface ShipStage {
  /**
   * Half-width of the hull.
   */
  readonly w: number;

  /**
   * Height of the hull.
   */
  readonly h: number;

  /**
   * Fin width.
   */
  readonly fins: number;

  /**
   * Booster height.
   */
  readonly boost: number;

  /**
   * Shape of the nose.
   */
  readonly nose: 'none' | 'dome' | 'cone' | 'capsule';

  /**
   * Engine count.
   */
  readonly eng: number;

  /**
   * Gantry height.
   */
  readonly gantry: number;

  /**
   * Porthole count.
   */
  readonly ports: number;

  /**
   * Marking band count.
   */
  readonly bands: number;
}

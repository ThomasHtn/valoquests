/**
 * One rocket stage; each defeated guardian adds a real part, not a scale-up.
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

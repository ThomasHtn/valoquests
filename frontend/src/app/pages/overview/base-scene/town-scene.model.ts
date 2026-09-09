/**
 * Inputs the scene is drawn from.
 */
export interface TownSceneInputs {
  /**
   * Inhabitants of the base.
   */
  readonly population: number;

  /**
   * Guardians defeated so far: one stage of the rocket each.
   */
  readonly stagesDone: number;

  /**
   * Population a campaign run to its end is expected to reach, the scale the city grows on.
   */
  readonly fullCampaignPopulation: number;

  /**
   * Whether the viewer prefers reduced motion; the lit windows then appear at once.
   */
  readonly reducedMotion: boolean;
}

/**
 * One building facade: its plot, its final size and the random draw styling it.
 */
export interface Facade {
  /**
   * Left edge, in viewBox units.
   */
  readonly x: number;

  /**
   * Bottom edge, in viewBox units.
   */
  readonly y: number;

  /**
   * Width, in viewBox units.
   */
  readonly w: number;

  /**
   * Height, in viewBox units.
   */
  readonly h: number;

  /**
   * Random draw in [0, 1) deciding the facade style.
   */
  readonly roll: number;
}

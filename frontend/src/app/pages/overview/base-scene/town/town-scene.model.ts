/**
 * Inputs the scene is drawn from.
 */
export interface TownSceneInputs {
  /**
   * Inhabitants of the base.
   */
  readonly population: number;

  /**
   * Population of the previous drawing; buildings grown since then rise (equal: no motion).
   */
  readonly previousPopulation: number;

  /**
   * Guardians defeated so far: one stage of the rocket each.
   */
  readonly stagesDone: number;

  /**
   * Population a full campaign should reach: the scale the city grows on.
   */
  readonly fullCampaignPopulation: number;

  /**
   * Drawing time in epoch ms: sets the light and the clouds.
   */
  readonly now: number;

  /**
   * Viewer prefers reduced motion: clouds, windows and rising buildings stand still.
   */
  readonly reducedMotion: boolean;
}

/**
 * Lot row: towers at the back, low quay houses at the front.
 */
export type LotRow = 'back' | 'front';

/**
 * One plot of the city: its place and when each building replaces the previous one.
 */
export interface Lot {
  /**
   * Stable identifier, seed of every random draw styling the lot.
   */
  readonly id: number;

  /**
   * Row the lot stands in.
   */
  readonly row: LotRow;

  /**
   * Left edge, in viewBox units.
   */
  readonly x: number;

  /**
   * Width, in viewBox units.
   */
  readonly w: number;

  /**
   * Growth reaching each tier, indexed by tier (first: lot built, last: final building).
   */
  readonly thresholds: readonly number[];

  /**
   * Tier height factor, so two lots of the same tier do not line up.
   */
  readonly scale: number;
}

/**
 * The building standing on a lot at a given tier.
 */
export interface BuildingShape {
  /**
   * Tier of the building, 0 for a cabin up to the skyscraper.
   */
  readonly tier: number;

  /**
   * Left edge, in viewBox units.
   */
  readonly x: number;

  /**
   * Width, in viewBox units.
   */
  readonly w: number;

  /**
   * Height of the body, roof excluded, in viewBox units.
   */
  readonly h: number;
}

/**
 * Rectangular volume of a building; a tower stacks several, set back.
 */
export interface BuildingVolume {
  /**
   * Left edge, in viewBox units.
   */
  readonly x: number;

  /**
   * Width, in viewBox units.
   */
  readonly w: number;

  /**
   * Top edge, in viewBox units.
   */
  readonly top: number;

  /**
   * Bottom edge, in viewBox units.
   */
  readonly bottom: number;
}

/**
 * How the windows of one volume are laid out: floors of evenly spaced columns.
 */
export interface WindowGrid {
  /**
   * Number of floors that fit in the volume, at least one.
   */
  readonly floors: number;

  /**
   * Height of one floor, in viewBox units.
   */
  readonly floorH: number;

  /**
   * Height of one window, in viewBox units.
   */
  readonly winH: number;

  /**
   * Number of windows per floor.
   */
  readonly cols: number;

  /**
   * Width of one window, in viewBox units.
   */
  readonly winW: number;

  /**
   * Gap between two windows of a floor, in viewBox units.
   */
  readonly gap: number;

  /**
   * Left edge of the first window (row centred in the volume), in viewBox units.
   */
  readonly left: number;
}

/**
 * Light and colours of the scene at one hour of the day.
 */
export interface SkyState {
  /**
   * Sky at the top of the frame.
   */
  readonly skyTop: string;

  /**
   * Sky two thirds of the way down.
   */
  readonly skyMid: string;

  /**
   * Sky at the horizon.
   */
  readonly skyLow: string;

  /**
   * Water right under the quay; it darkens toward the bottom whatever the hour.
   */
  readonly sea: string;

  /**
   * Hills behind the city.
   */
  readonly ridge: string;

  /**
   * Haze laid over the foot of the hills.
   */
  readonly haze: string;

  /**
   * Facade in shade.
   */
  readonly wall: string;

  /**
   * Facade in light.
   */
  readonly wallLit: string;

  /**
   * Roofs, caps and side shade.
   */
  readonly roof: string;

  /**
   * Window left unlit: dark at night, reflecting the sky by day.
   */
  readonly glass: string;

  /**
   * Body of the clouds.
   */
  readonly cloud: string;

  /**
   * Underside of the clouds.
   */
  readonly cloudShade: string;

  /**
   * Visibility of the stars, in [0, 1].
   */
  readonly stars: number;

  /**
   * Share of the windows lit, in [0, 1].
   */
  readonly lit: number;

  /**
   * How lit the street lamps and the pad lights are, in [0, 1].
   */
  readonly lamps: number;
}

/**
 * Sky at one hour; the scene interpolates between two keys.
 */
export interface SkyKey {
  /**
   * Hour of the day, fractional, in [0, 24].
   */
  readonly hour: number;

  /**
   * Light and colours at that hour.
   */
  readonly sky: SkyState;
}

/**
 * A body crossing the sky on an arc: the sun by day, the moon by night.
 */
export interface SkyBody {
  /**
   * Centre abscissa, in viewBox units.
   */
  readonly x: number;

  /**
   * Centre ordinate, in viewBox units.
   */
  readonly y: number;

  /**
   * Height above the horizon in [0, 1], 1 at the top of the arc.
   */
  readonly elevation: number;
}

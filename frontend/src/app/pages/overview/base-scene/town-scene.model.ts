/**
 * Inputs the scene is drawn from.
 */
export interface TownSceneInputs {
  /**
   * Inhabitants of the base.
   */
  readonly population: number;

  /**
   * Population the previous drawing showed: buildings grown since then rise out of the ground.
   * Equal to `population` when nothing should move.
   */
  readonly previousPopulation: number;

  /**
   * Guardians defeated so far: one stage of the rocket each.
   */
  readonly stagesDone: number;

  /**
   * Population a campaign run to its end is expected to reach, the scale the city grows on.
   */
  readonly fullCampaignPopulation: number;

  /**
   * Wall-clock time of the drawing, in epoch milliseconds: sets the light and the clouds.
   */
  readonly now: number;

  /**
   * Whether the viewer prefers reduced motion; clouds, blinking windows and rising buildings
   * then stand still.
   */
  readonly reducedMotion: boolean;
}

/**
 * Row a lot stands in: the back row holds the towers, the front row the low houses on the quay.
 */
export type LotRow = 'back' | 'front';

/**
 * One plot of the city: where it stands and when each of its buildings replaces the previous one.
 */
export interface Lot {
  /**
   * Stable identifier, the key of every random draw styling the lot.
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
   * Growth at which each tier is reached, index being the tier: the first entry is the day the lot
   * is built on, the last one the day it reaches its final building.
   */
  readonly thresholds: readonly number[];

  /**
   * Factor applied to the tier heights, so two lots of the same tier do not line up.
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
 * A rectangular volume of a building: a tower stacks several, set back one above the other.
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
 * The sky of one hour of the day, the scene interpolating between two of them.
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
   * Height above the horizon, in [0, 1]: 0 when rising or setting, 1 at the top of the arc.
   */
  readonly elevation: number;
}

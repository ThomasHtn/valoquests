import { PlanetState } from '../campaign.model';

/**
 * Geometry and palette of one planet of the strip, in its 100x100 viewBox.
 */

/**

 * Side of the square viewBox.

 */
export const ORB_VIEW_SIZE = 100;

/**

 * Centre of the orb.

 */
export const ORB_CX = 50;

/**
 * Vertical centre of the orb.
 */
export const ORB_CY = 50;

/**

 * Dark seas scattered on the ground.

 */
export const SEA_COUNT = 5;

/**

 * Ring colour per state of the planet.

 */
export const ORB_TONES: Readonly<Record<PlanetState, string>> = {
  won: '#e8ab6b',
  lost: '#e0404e',
  now: '#2dd4bf',
  ahead: '#5b7688',
};

/**

 * Fill and dotted stroke of a planet still ahead: a place, not a world yet.

 */
export const AHEAD_FILL = '#15222c';

/**
 * Dotted stroke of a planet still ahead.
 */
export const AHEAD_STROKE = '#33495b';

/**

 * Dark plate behind the ring, so the guardian's lines read on any hue.

 */
export const RING_PLATE = 'rgb(4 10 15 / 70%)';

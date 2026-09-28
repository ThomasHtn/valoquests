import {
  PLANET_ART_DISC_RATIO,
  PLANET_ART_EXTENT_RATIO,
} from '@core/campaign/planet-art.constants';
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
 * Ratio of the widest planetary ring to the globe: the guardian's ring is laid past it.
 */
export const RING_SCALE = PLANET_ART_EXTENT_RATIO / PLANET_ART_DISC_RATIO;

/**
 * Clearance between the widest planetary ring and the guardian's ring, in viewBox units.
 */
export const RING_GAP = 3;

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
 * A planet still ahead: its drawing in grey and faded, the same silhouette as on the overview.
 */
export const AHEAD_ART_STYLE = 'filter: grayscale(1); opacity: 0.38';

/**
 * Dark plate behind the ring, so the guardian's lines read on any planet.
 */
export const RING_PLATE = 'rgb(4 10 15 / 70%)';

/**
 * Plate of a defeated guardian's ring: empty, but amber so the planet reads as won.
 */
export const RING_PLATE_WON = 'rgb(217 149 74 / 45%)';

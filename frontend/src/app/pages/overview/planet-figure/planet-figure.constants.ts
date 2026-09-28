import {
  PLANET_ART_DISC_RATIO,
  PLANET_ART_EXTENT_RATIO,
} from '@core/campaign/planet-art.constants';

/**
 * Geometry and palette of the planet of the week, in the SVG viewBox's own units.
 */

/**
 * Side of the square viewBox.
 */
export const PLANET_VIEW_SIZE = 360;

/**
 * Centre of the globe.
 */
export const PLANET_CX = 180;

/**
 * Vertical centre of the globe.
 */
export const PLANET_CY = 180;

/**
 * Radius the guardian's ring is laid around.
 */
export const PLANET_RADIUS = 104;

/**
 * Side of the planet drawing, sized so a ringed planet's rings stop short of the guardian's ring.
 */
export const PLANET_ART_SIDE = Math.floor((PLANET_RADIUS + 14) / PLANET_ART_EXTENT_RATIO);

/**
 * Radius of the globe inside the drawing, where the wounded marks are laid.
 */
export const PLANET_DISC_RADIUS = PLANET_ART_SIDE * PLANET_ART_DISC_RATIO;

/**
 * Seed of the wounded marks, so the planet keeps its face across visits.
 */
export const PLANET_SEED = 815239;

/**
 * Amber marks laid on the lit face: the wounded, as a texture rather than a count.
 */
export const WOUNDED_MARKS = 26;

/**
 * Segments of the breakthrough ring, one per share of the guardian's hit points.
 */
export const RING_SEGMENTS = 26;

/**
 * Colours, aligned on the site palette.
 */
export const PLANET_COLORS = {
  warm: '#ffc477',
  warmCore: '#fff0cf',
  segmentAlive: '#e0404e',
  segmentDead: '#4a5560',
} as const;

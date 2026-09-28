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
 * Radius the guardian's standing segments start from.
 */
export const RING_INNER_RADIUS = PLANET_RADIUS + 20;

/**
 * Side of a ringed planet's drawing, sized so its rings stop short of the guardian's ring.
 */
export const RINGED_PLANET_ART_SIDE = Math.floor((PLANET_RADIUS + 14) / PLANET_ART_EXTENT_RATIO);

/**
 * Gap left between a bare globe and the guardian's ring.
 */
export const PLANET_RING_GAP = 22;

/**
 * Side of a bare planet's drawing: nothing reaches past its globe, so the globe grows up to
 * {@link PLANET_RING_GAP} short of the guardian's ring.
 */
export const PLANET_ART_SIDE = Math.floor(
  (RING_INNER_RADIUS - PLANET_RING_GAP) / PLANET_ART_DISC_RATIO,
);

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

import {
  PLANET_ART_DISC_RATIO,
  PLANET_ART_EXTENT_RATIO,
} from '@core/campaign/planets/campaign-planet-art.constants';

// Lengths in SVG viewBox units.

/**
 * Side of the square viewBox.
 */
export const PLANET_VIEW_SIZE = 360;

/**
 * Horizontal centre of the globe.
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
 * Side of a ringed planet's drawing, so its rings stop short of the guardian's.
 */
export const RINGED_PLANET_ART_SIDE = Math.floor((PLANET_RADIUS + 14) / PLANET_ART_EXTENT_RATIO);

/**
 * Gap left between a bare globe and the guardian's ring.
 */
export const PLANET_RING_GAP = 22;

/**
 * Side of a bare planet's drawing: the globe grows to {@link PLANET_RING_GAP} from the ring.
 */
export const PLANET_ART_SIDE = Math.floor(
  (RING_INNER_RADIUS - PLANET_RING_GAP) / PLANET_ART_DISC_RATIO,
);

/**
 * Seed of the wounded marks, so the planet keeps its face across visits.
 */
export const PLANET_SEED = 815239;

/**
 * Amber marks on the lit face: the wounded as a texture, not a count.
 */
export const WOUNDED_MARKS = 26;

/**
 * Segments of the breakthrough ring.
 */
export const RING_SEGMENTS = 26;

/**
 * Colours aligned on the site palette, as literals because SVG attributes cannot read tokens.
 */
export const PLANET_COLORS = {
  warm: '#ffc477',
  warmCore: '#fff0cf',
  // `--color-boss-hp-edge`.
  segmentAlive: '#e0404e',
  segmentDead: '#4a5560',
} as const;

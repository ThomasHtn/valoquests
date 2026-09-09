/**
 * Geometry and palette of the planet of the week, in the SVG viewBox's own units.
 */

/** Side of the square viewBox. */
export const PLANET_VIEW_SIZE = 360;

/** Centre of the globe. */
export const PLANET_CX = 180;
export const PLANET_CY = 180;

/** Radius of the globe. */
export const PLANET_RADIUS = 104;

/** Seed of the relief and the wounded marks, so the planet keeps its face across visits. */
export const PLANET_SEED = 815239;

/** Dark patches cut to the disc, so the globe does not read as a marble. */
export const RELIEF_PATCHES = 11;

/** Amber marks laid on the lit face: the wounded, as a texture rather than a count. */
export const WOUNDED_MARKS = 26;

/** Segments of the breakthrough ring, one per share of the guardian's hit points. */
export const RING_SEGMENTS = 26;

/** Colours, aligned on the site palette. */
export const PLANET_COLORS = {
  brand: '#d9954a',
  warm: '#ffc477',
  warmCore: '#fff0cf',
  night: '#040a11',
  segmentAlive: '#e0404e',
  segmentDead: '#4a5560',
} as const;

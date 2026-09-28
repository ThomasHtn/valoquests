/**
 * Folder of the planet drawings under `public`, one SVG per week of the campaign.
 */
export const PLANET_ART_FOLDER = '/planets';

/**
 * Radius of the globe in a planet drawing, as a share of the drawing's side. The rest of the square
 * is left to the ringed planets' rings.
 */
export const PLANET_ART_DISC_RATIO = 0.36;

/**
 * Half-width of the widest planetary ring in a drawing, as a share of the drawing's side.
 */
export const PLANET_ART_EXTENT_RATIO = 0.49;

/**
 * The lit face of every drawing: a circle offset towards the top left, as shares of the globe's
 * radius. Past it, the terminator's shadow begins.
 */
export const PLANET_ART_LIT_FACE = { offset: -0.25, radius: 0.94 } as const;

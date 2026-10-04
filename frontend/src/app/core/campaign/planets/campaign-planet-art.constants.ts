/**
 * Folder of the planet SVGs under `public`, one per week.
 */
export const PLANET_ART_FOLDER = '/planets';

/**
 * Globe radius as a share of the drawing's side; the rest is left for rings.
 */
export const PLANET_ART_DISC_RATIO = 0.36;

/**
 * Half-width of the widest ring, as a share of the drawing's side.
 */
export const PLANET_ART_EXTENT_RATIO = 0.49;

/**
 * Ranks of the drawings whose rings reach out to `PLANET_ART_EXTENT_RATIO`.
 */
export const RINGED_PLANET_RANKS: readonly number[] = [9, 10];

/**
 * Lit face circle, offset top-left, as shares of the globe's radius.
 */
export const PLANET_ART_LIT_FACE = { offset: -0.25, radius: 0.94 } as const;

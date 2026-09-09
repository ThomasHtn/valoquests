/**
 * What the ring holds, in characters, measured against the 34px disc it encloses at `size-11`:
 * `text-2xs` runs ~6px per monospaced digit and `text-3xs tracking-tighter` ~5px, and a grouped
 * damage count spends a character on its thousands separator too. Past the wider of the two, no
 * type step left is still legible — a six-figure total is abbreviated instead.
 *
 * Characters the label holds at its base type step.
 */
export const LABEL_FITS_AT_BASE_SIZE = 4;

/**
 * Characters the ring still holds legibly once the label switches to `text-3xs tracking-tighter`.
 */
export const LABEL_FITS_IN_RING = 6;

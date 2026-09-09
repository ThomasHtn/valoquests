/**
 * Frame, density and palette of the sky behind the road of the planets.
 */

/** ViewBox of the sky, wide enough to be cropped with `slice` on every screen. */
export const SKY_WIDTH = 1600;
export const SKY_HEIGHT = 420;

/** Stars scattered across the frame. */
export const STAR_COUNT = 220;

/** Seed of the field, so the same sky comes back on every visit. */
export const SKY_SEED = 20260905;

/** Share of stars drawn larger. */
export const BRIGHT_STAR_SHARE = 0.1;

/** Colours, aligned on the site palette. */
export const SKY_COLORS = {
  night: '#040a11',
  brand: '#d9954a',
  haze: '#5a96be',
  star: '#cfe4ee',
} as const;

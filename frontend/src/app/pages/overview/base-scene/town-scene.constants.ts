import { ROCKET_PALETTE } from '@shared/rocket/rocket-drawing.constants';
/**
 * Palette of the scene, in the colours of the rest of the site.
 */
export const TOWN_PALETTE = {
  night: '#040a11',
  hazeFar: '#0f1d27',
  far: '#0b1620',
  wall: '#141b24',
  wallLit: '#1e2733',
  roof: '#0b1117',
  quayEdge: '#22303a',
  quayFace: '#111a20',
  ...ROCKET_PALETTE,
} as const;

/**

 * Seed of the sky and the facades, so the base keeps its face across visits.

 */
export const TOWN_SEED = 20260902;

/**
 * Width of the drawing frame, in viewBox units.
 */
export const TOWN_WIDTH = 1200;

/**
 * Height of the drawing frame, in viewBox units.
 */
export const TOWN_HEIGHT = 430;

/**
 * Y of the ground line the buildings stand on.
 */
export const HORIZON = 336;

/**
 * X of the rocket, the centre of the frame.
 */
export const RX = 600;

/**
 * Scale applied to the shared ship drawing in this frame.
 */
export const SHIP_SCALE = 0.86;

/**
 * The launch plot, kept free of any building.
 */
export const PLOT: readonly [number, number] = [500, 720];

/**
 * Finished city: x, width and final height of every plot; `1` flags a facade sign.
 *
 * The village steps down toward the plot: tall volumes stay at the edges and the rows nearest the
 * rocket are low, so it dominates without competition.
 */
export const SKYLINE: readonly (readonly [number, number, number, number?])[] = [
  [8, 76, 88],
  [82, 46, 128],
  [126, 58, 66],
  [182, 82, 170, 1],
  [262, 48, 96],
  [308, 62, 132],
  [356, 46, 78],
  [400, 44, 54],
  [442, 54, 34],
  [494, 40, 26],
  [716, 44, 28],
  [758, 52, 38],
  [808, 44, 62],
  [850, 50, 88],
  [900, 46, 122],
  [946, 78, 172, 1],
  [1026, 52, 104],
  [1078, 84, 160],
  [1160, 62, 92],
];

/**
 * Growth the farthest plot needs before it is built.
 */
export const SPREAD = 0.55;

/**
 * Growth a plot then takes to reach its final height.
 */
export const RISE = 0.38;

/**
 * Share of its height a plot already has the day it comes out of the ground.
 */
export const SEEDLING = 0.4;

/**
 * Clarity of the night sky, in [0, 1]: drives how many stars are drawn.
 */
export const CLARITY = 0.8;

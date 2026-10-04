import { ShipStage } from './rocket-drawing.model';

/**
 * Ship palette, in the site's colours.
 */
export const ROCKET_PALETTE = {
  mast: '#2b3a45',
  steel: '#25384a',
  steelDark: '#1a2531',
  steelLit: '#33495b',
  ghost: '#5b7688',
  warm: '#ffc477',
  warmCore: '#fff0cf',
  brand: '#d9954a',
  cyan: '#2dd4bf',
  red: '#ff4655',
  hull: '#e4e0d8',
  hullLit: '#ffffff',
  ink: '#16202a',
  nozzle: '#0c141c',
  shade: '#0b1117',
} as const;

/**
 * Parts of the finished launcher, one per guardian.
 */
export const ROCKET_PART_COUNT = 10;

/**
 * Ship stages, index zero being nothing built.
 */
export const SHIP: readonly ShipStage[] = [
  { w: 0, h: 0, fins: 0, boost: 0, nose: 'none', eng: 0, gantry: 0, ports: 0, bands: 0 },
  { w: 11, h: 32, fins: 0, boost: 0, nose: 'dome', eng: 1, gantry: 0, ports: 0, bands: 0 },
  { w: 13, h: 52, fins: 1, boost: 0, nose: 'dome', eng: 1, gantry: 0, ports: 0, bands: 0 },
  { w: 15, h: 76, fins: 1, boost: 0, nose: 'cone', eng: 1, gantry: 0, ports: 1, bands: 0 },
  { w: 17, h: 104, fins: 1, boost: 0, nose: 'cone', eng: 1, gantry: 0, ports: 1, bands: 0 },
  { w: 19, h: 134, fins: 1, boost: 60, nose: 'cone', eng: 1, gantry: 0, ports: 2, bands: 0 },
  { w: 21, h: 164, fins: 1, boost: 82, nose: 'cone', eng: 3, gantry: 0, ports: 2, bands: 1 },
  { w: 22, h: 194, fins: 1, boost: 104, nose: 'cone', eng: 3, gantry: 1, ports: 2, bands: 1 },
  { w: 24, h: 222, fins: 1, boost: 126, nose: 'cone', eng: 3, gantry: 1, ports: 3, bands: 1 },
  { w: 25, h: 250, fins: 1, boost: 146, nose: 'capsule', eng: 3, gantry: 2, ports: 3, bands: 1 },
  { w: 27, h: 282, fins: 1, boost: 168, nose: 'capsule', eng: 3, gantry: 2, ports: 4, bands: 1 },
];

/**
 * Height of the engine skirt under the hull.
 */
export const SKIRT = 14;

/**
 * Gap between the ship's widest point and the service gantry.
 */
export const GANTRY_GAP = 22;

/**
 * Vertical spacing of the gantry's cross braces.
 */
export const GANTRY_BRACE_STEP = 14;

/**
 * Heights of the service arms reaching the hull, per gantry stage.
 */
export const GANTRY_ARM_HEIGHTS: Readonly<Record<number, readonly number[]>> = {
  1: [26, 78],
  2: [26, 74, 122, 170],
};

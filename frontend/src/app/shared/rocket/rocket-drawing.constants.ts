import { ShipStage } from './rocket-drawing.model';

/**
 * Palette of the ship, in the colours of the rest of the site.
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
} as const;

/**
 * Number of parts the finished launcher has: one per guardian of the campaign.
 */
export const ROCKET_PART_COUNT = 10;

/**
 * The ten stages of the ship, index zero being nothing built.
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
 * Engine skirt, under the hull.
 */
export const SKIRT = 14;

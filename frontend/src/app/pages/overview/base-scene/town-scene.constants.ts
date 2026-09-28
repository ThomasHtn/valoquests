import { ROCKET_PALETTE } from '@shared/rocket/rocket-drawing.constants';
import { LotRow, SkyKey, SkyState } from './town-scene.model';

/**
 * Colours of the scene that do not follow the hour, in the colours of the rest of the site.
 */
export const TOWN_PALETTE = {
  night: '#040a11',
  seaDeep: '#050f15',
  quayEdge: '#22303a',
  quayFace: '#111a20',
  padDeck: '#18232e',
  padEdge: '#2a3947',
  lampOff: '#4a5663',
  moon: '#dfe8ec',
  sunHigh: '#fff1d6',
  sunLow: '#ff9a5c',
  dayGlint: '#e2eef2',
  vapor: '#dfe8ec',
  ...ROCKET_PALETTE,
} as const;

/**
 * Paints the houses and blocks are coloured with, mixed into the hour's facade tone: brick,
 * terracotta, ochre, sage, slate blue, plum. Muted so they stay in the site's palette.
 */
export const FACADE_TINTS: readonly string[] = [
  '#8a4f3f',
  '#a0664a',
  '#a08448',
  '#617d68',
  '#4f6f8c',
  '#6f5a78',
];

/**
 * Share of the lots left in bare concrete.
 */
export const CONCRETE_SHARE = 0.3;

/**
 * Terracotta of the pitched roofs.
 */
export const ROOF_TILE = '#7d3f2f';

/**
 * Seed of the stars, the hills and the city plan, so the base keeps its face across visits.
 */
export const TOWN_SEED = 20260902;

/**
 * Width of the drawing frame, in viewBox units.
 */
export const TOWN_WIDTH = 1200;

/**
 * Height of the drawing frame, in viewBox units.
 */
export const TOWN_HEIGHT = 322;

/**
 * Y of the ground line the buildings stand on; the water below keeps its former 94 units.
 */
export const HORIZON = 228;

/**
 * X of the rocket, the centre of the frame.
 */
export const RX = 600;

/**
 * Scale applied to the shared ship drawing, so the finished launcher fits under the lowered sky.
 */
export const SHIP_SCALE = 0.52;

/**
 * Half-width of the launch pad deck.
 */
export const PAD_HALF = 112;

/**
 * The launch plot, kept free of any building: the rocket stands against the sky, never against roofs.
 */
export const PLOT: readonly [number, number] = [RX - PAD_HALF - 8, RX + PAD_HALF + 8];

/**
 * Exponent turning population share into growth: below 1, the first thousands build more, so the
 * early colony changes almost daily instead of sitting still for weeks.
 */
export const GROWTH_CURVE = 0.8;

/**
 * Growth at which the last building of the plan goes up, kept under 1 so a full campaign ends on
 * a finished city.
 */
export const COMPLETE_AT = 0.97;

/**
 * Buildings standing from day one: the founding camp around the pad.
 */
export const FOUNDING_CAMP = 4;

/**
 * Order key the farthest lot is built on at, the nearest being built on at zero.
 */
export const SPREAD = 0.62;

/**
 * Order key between two tiers of the same lot: small enough that a camp becomes a village fast,
 * large enough that towers wait for the second half of the campaign.
 */
export const TIER_PACE = 0.13;

/**
 * Body height of each tier, cabin to skyscraper, before the lot's own scale.
 */
export const TIER_HEIGHTS: readonly number[] = [12, 22, 42, 70, 112, 156];

/**
 * Highest tier, the skyscraper.
 */
export const TOP_TIER = TIER_HEIGHTS.length - 1;

/**
 * Tier of the tower, the first one with a setback and ribbon windows.
 */
export const TOWER_TIER = 4;

/**
 * Plot sizes of a row, in viewBox units.
 */
export const ROW_LAYOUT: Readonly<
  Record<LotRow, { start: number; minW: number; maxW: number; minGap: number; maxGap: number }>
> = {
  back: { start: -8, minW: 32, maxW: 50, minGap: 1, maxGap: 6 },
  front: { start: 6, minW: 22, maxW: 36, minGap: 4, maxGap: 9 },
};

/**
 * Darkness of the side fade at the very edge of the frame.
 */
export const SIDE_FADE_OPACITY = 0.4;

/**
 * Share of the frame width each side fade spans.
 */
export const SIDE_FADE_WIDTH = 0.16;

/**
 * Stops drawing the easing curve of each side fade.
 */
export const SIDE_FADE_STEPS = 8;

/**
 * Wall-clock refresh of the light, in milliseconds.
 */
export const CLOCK_TICK_MS = 5 * 60_000;

/**
 * Sunrise and sunset, in fractional hours.
 */
export const SUN_HOURS: readonly [number, number] = [6.6, 19.4];

/**
 * Moonrise and moonset, in fractional hours; the moon sets the morning after.
 */
export const MOON_HOURS: readonly [number, number] = [19.8, 30.2];

/**
 * Highest point the sun and the moon reach, in viewBox units.
 */
export const ARC_TOP = 30;

/**
 * Clouds drawn a given day: fewest and most.
 */
export const CLOUD_RANGE: readonly [number, number] = [2, 6];

/**
 * Time a cloud takes to cross the frame, in seconds: shortest and longest.
 */
export const CLOUD_CROSSING_S: readonly [number, number] = [260, 520];

/**
 * Deep night, the reference the other hours are tinted from.
 */
const NIGHT: SkyState = {
  skyTop: '#040a11',
  skyMid: '#08131c',
  skyLow: '#132430',
  sea: '#123340',
  ridge: '#0b1620',
  haze: '#0f1d27',
  wall: '#141b24',
  wallLit: '#1e2733',
  roof: '#0b1117',
  glass: '#0d1720',
  cloud: '#111d28',
  cloudShade: '#0b151e',
  stars: 1,
  lit: 0.3,
  lamps: 1,
};

/**
 * Full daylight: a muted slate-blue rather than a postcard blue, so the scene still sits on a dark site.
 */
const DAY: SkyState = {
  skyTop: '#275a80',
  skyMid: '#5b92b0',
  skyLow: '#a9c9d3',
  sea: '#3a6f82',
  ridge: '#4a6a7b',
  haze: '#8cadb9',
  wall: '#384955',
  wallLit: '#475a66',
  roof: '#26323b',
  glass: '#5a7888',
  cloud: '#e8eef0',
  cloudShade: '#b0c3cb',
  stars: 0,
  lit: 0.05,
  lamps: 0,
};

/**
 * Keys of the day, the scene interpolating between the two surrounding the current hour.
 */
export const SKY_KEYS: readonly SkyKey[] = [
  { hour: 0, sky: { ...NIGHT, lit: 0.44 } },
  { hour: 3, sky: { ...NIGHT, lit: 0.28 } },
  { hour: 5, sky: { ...NIGHT, lit: 0.28 } },
  {
    hour: 6.6,
    sky: {
      skyTop: '#1b2c47',
      skyMid: '#4d5a78',
      skyLow: '#d9977a',
      sea: '#3a4a5c',
      ridge: '#2a3448',
      haze: '#6a6478',
      wall: '#262e3e',
      wallLit: '#323b4c',
      roof: '#1a2130',
      glass: '#2a3345',
      cloud: '#e2a996',
      cloudShade: '#8d6f86',
      stars: 0.15,
      lit: 0.34,
      lamps: 0.6,
    },
  },
  {
    hour: 8.6,
    sky: {
      ...DAY,
      skyTop: '#2c5878',
      skyMid: '#5f8ba3',
      skyLow: '#b8cdd0',
      wall: '#34444f',
      wallLit: '#43545f',
      glass: '#56707e',
      cloud: '#dfe7ea',
      cloudShade: '#a9bcc4',
      lit: 0.07,
    },
  },
  { hour: 13, sky: DAY },
  {
    hour: 17.4,
    sky: {
      ...DAY,
      skyTop: '#2a5070',
      skyMid: '#6a8ea2',
      skyLow: '#d0bfa2',
      sea: '#4a6a74',
      ridge: '#53687a',
      haze: '#a09e9c',
      wall: '#34414c',
      wallLit: '#45505a',
      glass: '#5d6b76',
      cloud: '#eadbc8',
      cloudShade: '#a99a98',
      lit: 0.08,
    },
  },
  {
    hour: 19.3,
    sky: {
      skyTop: '#1c2447',
      skyMid: '#56416a',
      skyLow: '#e2885a',
      sea: '#4a3a52',
      ridge: '#2b2a42',
      haze: '#7a5570',
      wall: '#221f33',
      wallLit: '#2d2a40',
      roof: '#161424',
      glass: '#3a2f45',
      cloud: '#f0a57a',
      cloudShade: '#6e4a6c',
      stars: 0.1,
      lit: 0.46,
      lamps: 0.8,
    },
  },
  {
    hour: 20.6,
    sky: {
      skyTop: '#0b1528',
      skyMid: '#1a2d4c',
      skyLow: '#3e4f70',
      sea: '#1d3548',
      ridge: '#121d2e',
      haze: '#1f2d42',
      wall: '#171d2a',
      wallLit: '#212838',
      roof: '#0e131c',
      glass: '#141c28',
      cloud: '#25314a',
      cloudShade: '#182238',
      stars: 0.6,
      lit: 0.62,
      lamps: 1,
    },
  },
  { hour: 22, sky: { ...NIGHT, lit: 0.56 } },
  { hour: 24, sky: { ...NIGHT, lit: 0.44 } },
];

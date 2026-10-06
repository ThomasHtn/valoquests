import { ROCKET_PALETTE } from '@shared/rocket/rocket-drawing.constants';

import { LotRow, RowLayout, SkyKey, SkyState } from './town-scene.model';

/**
 * Scene colours that do not follow the hour (`night` is `--color-night-sky`, `star` `--color-starlight`).
 */
export const TOWN_PALETTE = {
  night: '#040a11',
  seaMid: '#081820',
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
  star: '#cfe4ee',
  ...ROCKET_PALETTE,
} as const;

/**
 * Muted facade paints, mixed into the hour's facade tone.
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
 * Seed of the stars, hills and city plan, so the base looks the same across visits.
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
 * Y of the ground line the buildings stand on.
 */
export const HORIZON = 228;

/**
 * Height of sky drawn above the frame, since phones crop to show the headroom.
 */
export const SKY_OVERDRAW = 60;

/**
 * X of the rocket, the centre of the frame.
 */
export const ROCKET_X = 600;

/**
 * Scale of the shared ship drawing, so the finished launcher fits under the sky.
 */
export const SHIP_SCALE = 0.52;

/**
 * Half-width of the launch pad deck.
 */
export const PAD_HALF_WIDTH = 112;

/**
 * Launch plot kept free of buildings, so the rocket stands against the sky.
 */
export const LAUNCH_PLOT: readonly [number, number] = [
  ROCKET_X - PAD_HALF_WIDTH - 8,
  ROCKET_X + PAD_HALF_WIDTH + 8,
];

/**
 * Population share to growth exponent; below 1 so the early colony changes almost daily.
 */
export const GROWTH_CURVE = 0.8;

/**
 * Lamp brightness above which lamps are drawn lit and the water reflects night lights.
 */
export const LAMPS_ON_THRESHOLD = 0.35;

/**
 * Distance between two lamps along the quay, in viewBox units.
 */
export const QUAY_LAMP_SPACING = 122;

/**
 * Distance either side of the launch plot kept free of quay lamps, in viewBox units.
 */
export const QUAY_LAMP_CLEARANCE = 40;

/**
 * Growth raising the last building; under 1 so a full campaign ends on a finished city.
 */
export const FINISHED_CITY_GROWTH = 0.97;

/**
 * Buildings standing from day one: the founding camp around the pad.
 */
export const FOUNDING_CAMP = 4;

/**
 * Order key of the farthest lot (the nearest is built at zero).
 */
export const FARTHEST_LOT_ORDER = 0.62;

/**
 * Order key between two tiers of a lot: fast villages, towers only in the second half.
 */
export const TIER_PACE = 0.13;

/**
 * Body height of each tier, cabin to skyscraper, before the lot's own scale.
 */
export const TIER_HEIGHTS: readonly number[] = [12, 22, 42, 70, 112, 156];

/**
 * Tier of the cabin, the smallest building, with a single window.
 */
export const CABIN_TIER = 0;

/**
 * Tier of the house, the last one with a pitched roof.
 */
export const HOUSE_TIER = 1;

/**
 * Tier of the small building: first with a cornice, crowned by a water tank.
 */
export const SMALL_BUILDING_TIER = 2;

/**
 * Tier of the block, crowned by a rooftop plant; windows stop growing from here.
 */
export const BLOCK_TIER = 3;

/**
 * Tier of the tower: first with a setback and ribbon windows.
 */
export const TOWER_TIER = 4;

/**
 * Highest tier, the skyscraper.
 */
export const TOP_TIER = TIER_HEIGHTS.length - 1;

/**
 * Plot sizes of a row, in viewBox units.
 */
export const ROW_LAYOUT: Readonly<Record<LotRow, RowLayout>> = {
  back: { start: -8, minWidth: 32, maxWidth: 50, minGap: 1, maxGap: 6 },
  front: { start: 6, minWidth: 22, maxWidth: 36, minGap: 4, maxGap: 9 },
};

/**
 * Time a building takes to rise to its new height, in milliseconds.
 */
export const RISE_DURATION_MS = 1100;

/**
 * Pause before the first building rises, in milliseconds.
 */
export const RISE_DELAY_MS = 400;

/**
 * Total time the rising buildings are spread over, in milliseconds.
 */
export const RISE_SPREAD_MS = 2400;

/**
 * Longest wait between two rising buildings, in milliseconds.
 */
export const RISE_MAX_STAGGER_MS = 160;

/**
 * Darkness of the side fade at the very edge of the frame.
 */
export const SIDE_FADE_OPACITY = 0.4;

/**
 * Share of the frame width each side fade spans.
 */
export const SIDE_FADE_WIDTH = 0.16;

/**
 * Gradient stops drawing each side fade's easing curve.
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
export const SKY_ARC_TOP = 30;

/**
 * Clouds drawn a given day: fewest and most.
 */
export const CLOUD_RANGE: readonly [number, number] = [2, 6];

/**
 * Time a cloud takes to cross the frame, in seconds: shortest and longest.
 */
export const CLOUD_CROSSING_S: readonly [number, number] = [260, 520];

/**
 * Deep night, the reference the other hours are tinted from (`skyTop` is `--color-night-sky`).
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
 * Full daylight, muted slate blue so the scene still sits on a dark site.
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
 * Sky keys of the day; the scene interpolates between the two around the current hour.
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

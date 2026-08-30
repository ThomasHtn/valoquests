import { ColonyTierName } from '@core/colony/colony.model';

/**
 * One primitive of the scene, in scene coordinates.
 *
 * The drawing is emitted as data rather than written into the template: a waterfront with twelve
 * growth stages, three terraces, reflections and four kinds of crossing is far past what `@for` over
 * hand-written markup stays readable at, and keeping the geometry here is what makes each stage
 * verifiable on its own.
 */
export type TownShape =
  | {
      readonly kind: 'rect';
      readonly fill: string;
      readonly opacity?: number;
      readonly x: number;
      readonly y: number;
      readonly w: number;
      readonly h: number;
    }
  | {
      readonly kind: 'poly';
      readonly fill: string;
      readonly opacity?: number;
      readonly points: string;
    }
  | { readonly kind: 'path'; readonly fill: string; readonly opacity?: number; readonly d: string }
  | {
      readonly kind: 'ellipse';
      readonly fill: string;
      readonly opacity?: number;
      readonly cx: number;
      readonly cy: number;
      readonly rx: number;
      readonly ry: number;
    }
  | {
      readonly kind: 'line';
      readonly stroke: string;
      readonly width: number;
      readonly opacity?: number;
      readonly x1: number;
      readonly y1: number;
      readonly x2: number;
      readonly y2: number;
    }
  | {
      readonly kind: 'arc';
      readonly stroke: string;
      readonly width: number;
      readonly opacity?: number;
      readonly d: string;
    };

/**
 * A building, kept apart from the rest so the template can stagger their entrance and so the water
 * can smear a reflection under each one.
 */
export interface TownBuilding {
  readonly x: number;
  readonly w: number;
  readonly shapes: readonly TownShape[];
}

/**
 * The whole scene, in the order it is painted: what is behind the frontage, the frontage itself,
 * then what stands in front of it.
 */
export interface TownScene {
  readonly viewBox: string;

  /** The rest of the city, held back by haze, behind everything else. */
  readonly backdrop: readonly TownShape[];

  /**
   * The terraces the frontage is built on. Carried apart from the backdrop because they are drawn
   * at the frontage's own offset: ground and buildings have to move together, or a house ends up
   * standing in the air above the terrace below its own.
   */
  readonly terraces: readonly TownShape[];

  /** The frontage, one entry per building, already translated to its place on the quay. */
  readonly buildings: readonly TownBuilding[];

  /** Street furniture, drawn with the frontage so it follows the same terraces. */
  readonly furniture: readonly TownShape[];

  /** Quay, water, reflections and the crossing. */
  readonly foreground: readonly TownShape[];

  /** Horizontal offset the frontage and its reflections are drawn at. */
  readonly frontageOffset: number;
}

/**
 * Daylight palette. The scene is lit at noon on purpose — the night version read as a ruin, and the
 * colony is meant to look like somewhere people live.
 *
 * Spelled out here rather than pulled from `colors.css`: these are the scene's own materials
 * (render, slate, glass, silt, water), not application chrome, and none of them appears anywhere
 * else in the interface. The two that do are the amber of `brand-500` on the landmark's crown and
 * the teal of `accent-cyan` on the roof collectors, which is why those two carry the exact tokens.
 */
const C = {
  skyTop: '#4d6b7b',
  skyMid: '#73858c',
  skyLow: '#8c9088',

  hazeFar: '#6d7f8a',
  hazeNear: '#596e7b',

  groundFace: ['#5f6d5c', '#55624f', '#4b5745'],
  groundCap: ['#7d8c72', '#71805f', '#657356'],
  retaining: '#414c3c',

  render: '#c2bbaa',
  renderLit: '#e0dac9',
  renderShade: '#b0a897',
  renderDark: '#8b8474',
  roof: '#525f68',
  roofShade: '#3f4a52',
  fascia: '#6c7a83',
  wood: '#7a6a52',
  woodDark: '#5b4c39',
  woodLight: '#8a7659',
  boarded: '#8a7a5e',
  boardLine: '#6f6249',
  doorway: '#4c4334',

  concrete: '#4a5f6b',
  concreteDark: '#3f5866',
  concreteDeep: '#3a5261',
  glass: '#5d7f92',
  glassBand: '#263944',
  pier: '#8f9aa2',
  parapet: '#9aa4ab',
  plantRoom: '#7c878f',
  mast: '#a9b3ba',
  shopfront: '#2c414d',
  brand: '#d9954a',
  collector: '#2dd4bf',

  silt: '#9e9470',
  siltFace: '#8a7f5e',
  reed: '#5e6b3f',
  quayEdge: '#8b959c',
  quayFace: '#4c5a52',
  water: '#26505f',
  waterLine: '#7fb0bf',
  ripple: '#8fc0cf',
  reflection: '#cfe0e6',

  stone: '#9a9488',
  stoneLight: '#bdb7a9',
  stoneParapet: '#a9a396',
  stoneBaluster: '#7f7a70',
  archShadow: '#1d4250',
  steel: '#4d5b63',
  stay: '#aab4ba',
  pilePier: '#3d4a51',

  lamp: '#8b959c',
  lampHead: '#a9b3ba',
  trunk: '#4a4436',
  foliage: '#48604a',
  foliageLit: '#587a58',
  fencePost: '#6f6249',
  track: '#8a7f5e',
  trackCap: '#a2966f',
  rut: '#6f6549',
  ford: '#9a9078',
} as const;

/**
 * How far past the frame every full-bleed band is drawn.
 *
 * SVG clips to the viewport, not to the `viewBox`, so a band drawn this wide still fills a very wide
 * panel once `xMidYMax meet` has scaled the scene to its height. That is what lets the water, the
 * terraces and the bridge reach the panel's edges without a second, viewport-sized drawing.
 */
const FAR = 2600;

/** The quay's own level, the waterline, and the bridge deck. */
const QUAY_Y = 250;
const WATER_Y = 266;
const DECK_Y = 296;
const SCENE_W = 720;
const SCENE_H = 340;

/**
 * The terraces, from the back of the scene down to the quay: where each one ends, and how high it
 * stands above the quay. A single flat baseline is what made the first pass read as a strip of
 * buildings rather than as a place.
 */
const TERRACES: readonly { readonly until: number; readonly lift: number }[] = [
  { until: 300, lift: 36 },
  { until: 470, lift: 20 },
  { until: Number.POSITIVE_INFINITY, lift: 0 },
];

const liftAt = (x: number): number => (TERRACES.find((step) => x < step.until) ?? TERRACES[2]).lift;

/**
 * Build stage each step of the ladder is drawn at, `0`–`11`.
 *
 * One stage per named step rather than one per glyph band: the ladder is the run's whole arc, and a
 * step that changes nothing in the scene is a step the page cannot show the squad it has taken.
 */
const TIER_STAGES: Readonly<Record<ColonyTierName, number>> = {
  CAMP: 0,
  HAMLET: 1,
  VILLAGE: 2,
  BOROUGH: 3,
  TOWN: 4,
  CITY: 5,
  RESIDENTIAL_QUARTER: 6,
  GREAT_CITY: 7,
  METROPOLIS: 8,
  MEGALOPOLIS: 9,
  CAPITAL: 10,
  CITADEL: 11,
};

/**
 * Resolves the build stage a step of the ladder is drawn at.
 *
 * @param tier - Name of the step the colony currently stands on.
 * @returns Its stage, `0`–`11`.
 */
export function townStageFor(tier: ColonyTierName): number {
  return TIER_STAGES[tier];
}

/** Where the water starts, where the quay is masonry rather than silt, and what crosses it. */
const WATER_STAGE = 1;
const QUAY_STAGE = 3;
const LAMP_STAGE = 2;

type Crossing = 'none' | 'ford' | 'plank' | 'stone' | 'cable';

function crossingAt(stage: number): Crossing {
  if (stage < WATER_STAGE) {
    return 'none';
  }
  if (stage >= 5) {
    return 'cable';
  }
  if (stage >= 3) {
    return 'stone';
  }
  return stage >= 2 ? 'plank' : 'ford';
}

const rect = (
  fill: string,
  x: number,
  y: number,
  w: number,
  h: number,
  opacity?: number,
): TownShape => ({ kind: 'rect', fill, x, y, w, h, opacity });

const band = (fill: string, y: number, h: number, opacity?: number): TownShape =>
  rect(fill, -FAR, y, FAR * 2, h, opacity);

const poly = (fill: string, points: string, opacity?: number): TownShape => ({
  kind: 'poly',
  fill,
  points,
  opacity,
});

const line = (
  stroke: string,
  width: number,
  x1: number,
  y1: number,
  x2: number,
  y2: number,
  opacity?: number,
): TownShape => ({ kind: 'line', stroke, width, x1, y1, x2, y2, opacity });

/**
 * Glass reads as glass by what it reflects: the sky, cut by a diagonal highlight over horizontal
 * floor bands. Never a grid of lit windows — one lit window is the single thing that would drag the
 * scene back to the night version it replaces.
 */
function glazing(x: number, y: number, w: number, h: number, tint?: string): TownShape[] {
  const shapes: TownShape[] = [rect(tint ?? C.glass, x, y, w, h)];

  for (let bandY = y + 5; bandY < y + h - 2; bandY += 9) {
    shapes.push(rect(C.glassBand, x, bandY, w, 1.6, 0.85));
  }

  shapes.push(
    poly(
      '#ffffff',
      `${x},${y + h * 0.78} ${x + w},${y + h * 0.2} ${x + w},${y + h * 0.42} ${x},${y + h}`,
      0.13,
    ),
  );

  return shapes;
}

function mast(x: number, top: number, length: number): TownShape[] {
  return [
    line(C.mast, 2.4, x, top, x, top - length),
    line(C.mast, 1.6, x - 5, top - length * 0.55, x + 5, top - length * 0.55),
    { kind: 'ellipse', fill: C.brand, cx: x, cy: top - length, rx: 2.6, ry: 2.6 },
  ];
}

/** Parapet, plant room and, on some roofs, a pair of collectors: a roof that is maintained. */
function roofKit(x: number, w: number, top: number, solar: boolean): TownShape[] {
  const shapes: TownShape[] = [
    rect(C.parapet, x - 3, top - 4, w + 6, 4),
    rect(C.plantRoom, x + w * 0.1, top - 13, w * 0.28, 9),
  ];

  if (solar) {
    shapes.push(
      poly(
        C.collector,
        `${x + w * 0.46},${top - 6} ${x + w * 0.92},${top - 15} ${x + w * 0.92},${top - 10} ${x + w * 0.46},${top - 1}`,
        0.65,
      ),
    );
  }

  return shapes;
}

/** A glazed ground floor, which is what makes a street read as a street rather than as a plinth. */
function shopfront(x: number, w: number, base: number): TownShape[] {
  return [
    rect(C.shopfront, x + 3, base - 26, w - 6, 26),
    rect(C.parapet, x + 3, base - 26, w - 6, 2),
    rect(C.quayEdge, x - 2, base - 30, w + 4, 4),
  ];
}

/**
 * The colony's first houses: rendered walls, a slate gable, a door, a window and a chimney. Small,
 * but built to stay — the settlement is founded, not camped.
 */
function house(x: number, w: number, h: number, flip = false): TownShape[] {
  const base = QUAY_Y - liftAt(x);
  const wallH = h * 0.56;
  const eave = base - wallH;
  const ridge = base - h;
  const cx = x + w / 2;
  const chimneyX = x + w * (flip ? 0.16 : 0.68);
  const chimneyW = w * 0.15;
  const doorW = w * 0.2;
  const doorX = x + w * 0.13;
  const doorH = wallH * 0.58;
  const winW = w * 0.26;
  const winX = x + w * 0.47;
  const winY = eave + wallH * 0.24;
  const winH = wallH * 0.34;

  return [
    rect(C.renderShade, chimneyX, ridge + 4, chimneyW, wallH * 0.9),
    rect(C.renderDark, chimneyX - 1.5, ridge + 1, chimneyW + 3, 3.5),
    rect(C.render, x, eave, w, wallH),
    rect(C.renderLit, x, eave, w * 0.34, wallH),
    rect(C.renderDark, x, base - 4, w, 4),
    poly(C.roof, `${x - 5},${eave + 4} ${cx},${ridge} ${x + w + 5},${eave + 4}`),
    poly(C.roofShade, `${cx},${ridge} ${x + w + 5},${eave + 4} ${cx},${eave + 4}`),
    rect(C.fascia, x - 5, eave + 4, w + 10, 3),
    rect(C.wood, doorX, base - doorH, doorW, doorH),
    rect(C.renderDark, doorX, base - doorH, doorW, 2),
    {
      kind: 'ellipse',
      fill: '#efe7d6',
      cx: doorX + doorW * 0.78,
      cy: base - doorH * 0.45,
      rx: 1.1,
      ry: 1.1,
    },
    rect(C.renderDark, winX - 2, winY - 2, winW + 4, winH + 4),
    rect(C.glass, winX, winY, winW, winH),
    rect(C.renderLit, winX + winW / 2 - 0.8, winY, 1.6, winH),
    rect(C.renderLit, winX, winY + winH / 2 - 0.8, winW, 1.6),
  ];
}

/** Boarded walls under a single sloping roof: a store, not a dwelling. */
function shed(x: number, w: number, h: number, flip = false): TownShape[] {
  const base = QUAY_Y - liftAt(x);
  const high = base - h;
  const low = base - h * 0.66;
  const leftY = flip ? high : low;
  const rightY = flip ? low : high;
  const shapes: TownShape[] = [
    poly(C.boarded, `${x},${base} ${x},${leftY} ${x + w},${rightY} ${x + w},${base}`),
  ];

  for (let boardX = x + 5; boardX < x + w - 2; boardX += 6) {
    const t = (boardX - x) / w;
    shapes.push(line(C.boardLine, 1.1, boardX, base, boardX, leftY + (rightY - leftY) * t, 0.6));
  }

  shapes.push(
    poly(
      C.roofShade,
      `${x - 4},${leftY + 2} ${x + w + 4},${rightY + 2} ${x + w + 4},${rightY - 3} ${x - 4},${leftY - 3}`,
    ),
    rect(C.doorway, x + w * 0.34, base - h * 0.42, w * 0.32, h * 0.42),
  );

  return shapes;
}

/** Masonry base, one glazed band, flat roof: the step between the houses and the towers. */
function lowRise(x: number, w: number, h: number): TownShape[] {
  const base = QUAY_Y - liftAt(x);

  return [
    rect(C.render, x, base - h, w, h),
    rect('#ffffff', x, base - h, w * 0.2, h, 0.14),
    ...glazing(x + w * 0.1, base - h * 0.82, w * 0.8, h * 0.3),
    ...shopfront(x, w, base),
    ...roofKit(x, w, base - h, false),
  ];
}

function midRise(x: number, w: number, h: number): TownShape[] {
  const base = QUAY_Y - liftAt(x);

  return [
    rect(C.concrete, x, base - h, w, h),
    ...glazing(x + 4, base - h + 6, w - 8, h - 40, '#54788c'),
    ...shopfront(x, w, base),
    ...roofKit(x, w, base - h, false),
  ];
}

/** A straight shaft, fully glazed on structural piers, capped and sometimes masted. */
function tower(
  x: number,
  w: number,
  h: number,
  options: { readonly tint?: string; readonly mast?: number; readonly solar?: boolean } = {},
): TownShape[] {
  const base = QUAY_Y - liftAt(x);
  const shapes: TownShape[] = [
    rect(C.concreteDark, x, base - h, w, h),
    ...glazing(x + 3, base - h + 4, w - 6, h - 34, options.tint),
  ];

  for (let i = 1; i < 3; i++) {
    shapes.push(rect(C.pier, x + (w * i) / 3 - 1.2, base - h + 4, 2.4, h - 34, 0.55));
  }

  shapes.push(...shopfront(x, w, base), ...roofKit(x, w, base - h, options.solar ?? false));

  if (options.mast) {
    shapes.push(...mast(x + w / 2, base - h - 13, options.mast));
  }

  return shapes;
}

/** Two stacked volumes, the upper one narrower. */
function setback(x: number, w: number, h: number, solar = false): TownShape[] {
  const base = QUAY_Y - liftAt(x);
  const upperW = w * 0.62;
  const upperX = x + (w - upperW) / 2;
  const lower = h * 0.5;

  return [
    rect(C.concreteDark, x, base - lower, w, lower),
    ...glazing(x + 3, base - lower + 4, w - 6, lower - 34),
    rect(C.parapet, x - 3, base - lower - 4, w + 6, 4),
    rect(C.concreteDark, upperX, base - h, upperW, h - lower),
    ...glazing(upperX + 3, base - h + 4, upperW - 6, h - lower - 8),
    ...shopfront(x, w, base),
    ...roofKit(upperX, upperW, base - h, solar),
    ...mast(upperX + upperW / 2, base - h - 13, 26),
  ];
}

/** The landmark: the tallest shaft, a crown band in the application's amber, a spire. */
function spire(x: number, w: number, h: number): TownShape[] {
  const base = QUAY_Y - liftAt(x);

  return [
    poly(
      C.concreteDeep,
      `${x},${base} ${x + w},${base} ${x + w - 5},${base - h} ${x + 5},${base - h}`,
    ),
    ...glazing(x + 8, base - h + 6, w - 16, h - 46, '#65899d'),
    rect(C.parapet, x + 3, base - h - 7, w - 6, 7),
    rect(C.brand, x + 6, base - h - 5, w - 12, 3),
    poly(
      C.pier,
      `${x + w / 2 - 5},${base - h - 7} ${x + w / 2 + 5},${base - h - 7} ${x + w / 2},${base - h - 40}`,
    ),
    ...shopfront(x, w, base),
    ...mast(x + w / 2, base - h - 40, 22),
  ];
}

/** A glazed link between two towers, which is what makes the upper city read as connected. */
function skybridge(x1: number, x2: number, up: number): TownShape[] {
  const base = QUAY_Y - liftAt(x1);

  return [
    rect(C.concreteDark, x1, base - up, x2 - x1, 11),
    rect('#7fa4b6', x1, base - up + 3, x2 - x1, 4, 0.6),
  ];
}

/**
 * The frontage, stage by stage. `until` retires a form the colony has outgrown: the first houses
 * stand through the borough, then the waterfront is rebuilt over them.
 */
interface FrontageEntry {
  readonly stage: number;
  readonly until?: number;
  readonly x: number;
  readonly w: number;
  readonly draw: () => readonly TownShape[];
}

const FRONTAGE: readonly FrontageEntry[] = [
  { stage: 0, until: 3, x: 22, w: 30, draw: () => shed(22, 30, 26) },
  { stage: 0, until: 3, x: 58, w: 40, draw: () => house(58, 40, 38) },
  { stage: 0, until: 3, x: 104, w: 34, draw: () => house(104, 34, 31, true) },
  { stage: 0, until: 3, x: 144, w: 28, draw: () => shed(144, 28, 23, true) },
  { stage: 1, x: 176, w: 56, draw: () => lowRise(176, 56, 48) },
  { stage: 2, x: 234, w: 50, draw: () => lowRise(234, 50, 42) },
  { stage: 3, x: 302, w: 52, draw: () => midRise(302, 52, 78) },
  { stage: 4, x: 356, w: 46, draw: () => midRise(356, 46, 64) },
  { stage: 5, x: 404, w: 40, draw: () => tower(404, 40, 104, { tint: '#4f7286' }) },
  { stage: 6, x: 428, w: 36, draw: () => tower(428, 36, 88, { solar: true }) },
  { stage: 7, x: 474, w: 50, draw: () => setback(474, 50, 138) },
  { stage: 8, x: 526, w: 38, draw: () => tower(526, 38, 118, { mast: 18, tint: '#5b7f92' }) },
  { stage: 9, x: 566, w: 58, draw: () => spire(566, 58, 188) },
  { stage: 10, x: 634, w: 40, draw: () => tower(634, 40, 126, { mast: 22, tint: '#4a6c80' }) },
  { stage: 10, x: 620, w: 18, draw: () => skybridge(620, 638, 96) },
  { stage: 11, x: 678, w: 44, draw: () => setback(678, 44, 140, true) },
  // by the last steps the waterfront runs off the frame on both sides
  { stage: 11, x: 2, w: 40, draw: () => tower(2, 40, 96, { mast: 16, tint: '#54788c' }) },
];

/**
 * The rest of the city, held back by haze. Deterministic: the skyline must not reshuffle itself on
 * every change detection pass.
 */
function hazeRow(
  step: number,
  minW: number,
  rangeW: number,
  minH: number,
  rangeH: number,
  fill: string,
  opacity: number,
  seed: number,
  lift: number,
): TownShape[] {
  const shapes: TownShape[] = [];
  let state = seed;
  const random = (): number => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return state / 2147483648;
  };

  for (let x = -FAR; x < FAR; x += step) {
    const w = minW + random() * rangeW;
    const h = minH + random() * rangeH;
    shapes.push(rect(fill, x, QUAY_Y - lift - h, w, h, opacity));

    if (random() > 0.74) {
      shapes.push(rect(fill, x + w * 0.3, QUAY_Y - lift - h - 13, w * 0.4, 13, opacity));
    }
  }

  return shapes;
}

/** The terraces themselves, and the retaining walls holding each one above the next. */
function terraces(): TownShape[] {
  const shapes: TownShape[] = [];
  let from = -FAR;

  TERRACES.forEach((step, index) => {
    const to = Math.min(step.until, FAR);
    const top = QUAY_Y - step.lift;

    shapes.push(rect(C.groundFace[index], from, top, to - from, SCENE_H - top));
    shapes.push(rect(C.groundCap[index], from, top, to - from, 4));

    if (index > 0) {
      const drop = TERRACES[index - 1].lift - step.lift;
      shapes.push(rect(C.retaining, from - 8, top - drop, 8, drop));
      shapes.push(rect(C.groundCap[index], from - 8, top - drop, 8, 3));
    }

    from = to;
  });

  return shapes;
}

/** Lamp posts are public works: they arrive with the first streets, not with the first houses. */
function furnitureFor(stage: number): TownShape[] {
  const shapes: TownShape[] = [];

  if (stage >= LAMP_STAGE) {
    for (let x = -FAR + 20; x < FAR; x += 168) {
      const base = QUAY_Y - liftAt(x);
      shapes.push(
        rect(C.lamp, x, base - 42, 3, 44),
        { kind: 'arc', stroke: C.lamp, width: 3, d: `M${x + 1.5},${base - 42} q0,-9 12,-9` },
        rect(C.lampHead, x + 9, base - 52, 11, 3.5),
      );
    }
  } else {
    // what a settlement has instead: a post-and-rail fence along the ridge
    for (let x = -FAR; x < FAR; x += 22) {
      const base = QUAY_Y - liftAt(x);
      shapes.push(
        rect(C.fencePost, x, base - 15, 2.4, 15),
        rect(C.wood, x, base - 12, 22, 2),
        rect(C.wood, x, base - 6, 22, 2),
      );
    }
  }

  for (let x = -FAR + 96; x < FAR; x += 168) {
    const base = QUAY_Y - liftAt(x);
    shapes.push(
      rect(C.trunk, x - 2, base - 26, 4, 28),
      { kind: 'ellipse', fill: C.foliage, cx: x, cy: base - 34, rx: 13, ry: 12 },
      { kind: 'ellipse', fill: C.foliageLit, cx: x - 5, cy: base - 40, rx: 8, ry: 7 },
    );
  }

  return shapes;
}

/** Planks on driven piles: the first thing the colony builds over the water. */
function plankCrossing(): TownShape[] {
  const shapes: TownShape[] = [];

  for (let x = -FAR + 30; x < FAR; x += 92) {
    shapes.push(
      rect(C.woodDark, x, DECK_Y + 5, 5, 44),
      rect(C.woodDark, x + 34, DECK_Y + 5, 5, 44),
      rect(C.woodDark, x, DECK_Y + 16, 39, 3),
    );
  }

  shapes.push(band(C.wood, DECK_Y, 7));

  for (let x = -FAR; x < FAR; x += 13) {
    shapes.push(rect(C.woodDark, x, DECK_Y, 1.3, 7, 0.55));
  }

  shapes.push(band(C.woodLight, DECK_Y - 17, 2.8));

  for (let x = -FAR + 18; x < FAR; x += 46) {
    shapes.push(rect(C.wood, x, DECK_Y - 17, 2.8, 17));
  }

  return shapes;
}

/**
 * Masonry piers, round arches, a parapet of balusters. The crossing becomes permanent at the same
 * step the frontage does. The arch springs low enough that its crown stays under the deck.
 */
function stoneCrossing(): TownShape[] {
  const span = 104;
  const radius = 32;
  const spring = SCENE_H - 8;
  const shapes: TownShape[] = [band(C.stone, DECK_Y, SCENE_H - DECK_Y)];

  for (let x = -FAR + 52; x < FAR; x += span) {
    shapes.push({
      kind: 'path',
      fill: C.archShadow,
      d: `M${x - radius},${SCENE_H} L${x - radius},${spring} A${radius},${radius} 0 0 1 ${x + radius},${spring} L${x + radius},${SCENE_H} Z`,
    });
    shapes.push({
      kind: 'arc',
      stroke: C.stoneLight,
      width: 4,
      d: `M${x - radius},${spring} A${radius},${radius} 0 0 1 ${x + radius},${spring}`,
    });
  }

  shapes.push(band(C.stoneParapet, DECK_Y - 15, 15));

  for (let x = -FAR; x < FAR; x += 24) {
    shapes.push(rect(C.stoneBaluster, x, DECK_Y - 11, 5, 11, 0.7));
  }

  shapes.push(band(C.stoneLight, DECK_Y - 18, 4));

  return shapes;
}

/** Cable-stayed: the one thing in the scene that is not a building. */
function cableCrossing(): TownShape[] {
  const shapes: TownShape[] = [band(C.steel, DECK_Y, 7), band(C.quayEdge, DECK_Y - 3, 3)];

  for (let x = -FAR + 150; x < FAR; x += 470) {
    const top = DECK_Y - 52;
    shapes.push(
      rect(C.pilePier, x - 6, DECK_Y + 7, 12, 34),
      rect(C.quayEdge, x - 3, top, 6, DECK_Y - top),
      rect(C.quayEdge, x - 8, top + 19, 16, 3.5),
    );

    for (let k = 1; k <= 4; k++) {
      const reach = k * 24;
      shapes.push(
        line(C.stay, 0.9, x, top + 3, x - reach, DECK_Y, 0.8),
        line(C.stay, 0.9, x, top + 3, x + reach, DECK_Y, 0.8),
      );
    }
  }

  return shapes;
}

/**
 * Builds the whole scene for one step of the ladder.
 *
 * @param stage - Build stage, `0`–`11`, from {@link townStageFor}.
 * @returns Everything the template draws, in painting order.
 */
export function buildTownScene(stage: number): TownScene {
  const visible = FRONTAGE.filter(
    (entry) => entry.stage <= stage && (entry.until === undefined || stage <= entry.until),
  );

  // The frontage is centred on what it actually contains, so a hamlet is not left in the corner of
  // an avenue it has not built yet.
  const minX = Math.min(...visible.map((entry) => entry.x));
  const maxX = Math.max(...visible.map((entry) => entry.x + entry.w));
  const frontageOffset = SCENE_W / 2 - (minX + maxX) / 2;

  const backdrop: TownShape[] = [];

  if (stage >= 4) {
    backdrop.push(...hazeRow(38, 22, 16, 50, 96, C.hazeFar, 0.3, 7, 44));
  }
  if (stage >= 6) {
    backdrop.push(...hazeRow(54, 28, 18, 34, 78, C.hazeNear, 0.4, 31, 38));
  }

  const foreground: TownShape[] = [];
  const hasWater = stage >= WATER_STAGE;

  if (hasWater) {
    if (stage >= QUAY_STAGE) {
      foreground.push(
        band(C.quayEdge, QUAY_Y, 6),
        band(C.quayFace, QUAY_Y + 6, WATER_Y - QUAY_Y - 6),
      );
    } else {
      // not built yet: silt and reeds down to the water
      foreground.push(band(C.silt, QUAY_Y, 5), band(C.siltFace, QUAY_Y + 5, WATER_Y - QUAY_Y - 5));

      for (let x = -FAR; x < FAR; x += 17) {
        const h = 7 + ((x * 31) % 9);
        foreground.push(line(C.reed, 1.4, x, WATER_Y + 1, x + 2, WATER_Y - h, 0.7));
      }
    }

    foreground.push(band(C.water, WATER_Y, SCENE_H - WATER_Y), band(C.waterLine, WATER_Y, 3, 0.55));

    for (let i = 0; i < 8; i++) {
      foreground.push(
        band(C.ripple, WATER_Y + 5 + i * 8, 1.6, Number((0.22 - i * 0.024).toFixed(3))),
      );
    }
  } else {
    // no water yet: a beaten track where the crossing will one day be
    foreground.push(band(C.track, DECK_Y - 8, 30), band(C.trackCap, DECK_Y - 8, 2));

    for (let x = -FAR; x < FAR; x += 26) {
      foreground.push(
        rect(C.rut, x, DECK_Y + 2, 14, 2, 0.6),
        rect(C.rut, x + 6, DECK_Y + 12, 14, 2, 0.6),
      );
    }
  }

  const crossing = crossingAt(stage);
  if (crossing === 'ford') {
    for (let x = -FAR; x < FAR; x += 21) {
      foreground.push({ kind: 'ellipse', fill: C.ford, cx: x, cy: DECK_Y + 22, rx: 7, ry: 3 });
    }
  } else if (crossing === 'plank') {
    foreground.push(...plankCrossing());
  } else if (crossing === 'stone') {
    foreground.push(...stoneCrossing());
  } else if (crossing === 'cable') {
    foreground.push(...cableCrossing());
  }

  return {
    viewBox: `0 0 ${SCENE_W} ${SCENE_H}`,
    backdrop,
    terraces: terraces(),
    buildings: visible.map((entry) => ({ x: entry.x, w: entry.w, shapes: entry.draw() })),
    furniture: furnitureFor(stage),
    foreground,
    frontageOffset,
  };
}

/**
 * The smear each building leaves on the water — a narrow one under each rather than a slab, so the
 * water still reads as water and the ripples cut across it.
 *
 * @param buildings - The placed frontage.
 * @returns One reflection per building, in the frontage's own coordinates.
 */
export function reflectionsFor(buildings: readonly TownBuilding[]): readonly TownShape[] {
  return buildings.map((building) =>
    rect(C.reflection, building.x + building.w * 0.25, WATER_Y, building.w * 0.5, 30, 0.13),
  );
}

/** Whether the scene has any water at this stage, which is what the reflections are drawn on. */
export function hasWaterAt(stage: number): boolean {
  return stage >= WATER_STAGE;
}

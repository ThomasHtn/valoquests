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

  /**
   * The moon. Its own layer because the readout plate covers the top of the sky on a phone, where it
   * is dropped rather than left as a smudge behind the population figure.
   */
  readonly moon: readonly TownShape[];

  /** The dome of light the city throws into its own sky. Painted before anything else. */
  readonly dome: readonly TownShape[];

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

  /**
   * The one sign in the scene on a failing ballast, kept out of the buildings so the template can
   * put it under its own animation. Drawn at the frontage's offset, after every building.
   */
  readonly flicker: readonly TownShape[];

  /** Street furniture, drawn with the frontage so it follows the same terraces. */
  readonly furniture: readonly TownShape[];

  /**
   * The quarter the colony is paying for right now, drawn whole and unlit.
   *
   * The template reveals it from the ground up in proportion to the materials banked towards the
   * next step, so the town gains a course of masonry on any day the squad brought some in rather
   * than a whole street once a week. Kept apart from {@link buildings} for exactly that reason: it
   * is the one group that is clipped.
   */
  readonly construction: readonly TownShape[];

  /** Quay, water, reflections and the crossing. */
  readonly foreground: readonly TownShape[];

  /** Horizontal offset the frontage and its reflections are drawn at. */
  readonly frontageOffset: number;
}

/**
 * Night palette. The scene used to be lit at noon; it is now lit by the colony itself, which is what
 * lets the drawing say something the daylight version could not — how many windows are on is the
 * population, so growth is visible inside the volumes and not only in their outline.
 *
 * One rule governs every saturated value, and it is the interface's own: **amber is life** (windows,
 * doorways, street lamps, the landmark's crown), **cyan is infrastructure** (signage, quay, bridge
 * lighting), **red is warning and nothing else** (mast beacons). Everything else is a material —
 * render, slate, glass, silt, water — and none of those appears anywhere else in the interface.
 */
const C = {
  hazeFar: '#0f1d27',
  hazeNear: '#132430',
  hazeVeil: '#0b1620',

  /** The dark the scene opens and closes on, at the very top of the sky and at the frame's foot. */
  night: '#040a11',

  /*
   * The ground, and one colour for all of it.
   *
   * It used to be three values falling away from the horizon, plus a lighter cap along each top
   * edge. On a panel four hundred pixels high that read as depth; on one holding a screen it read as
   * three different blacks meeting on two straight lines, with a bright lip drawn along every one of
   * them. The relief is still there — the terraces still step down — but it is carried by the
   * silhouette against the sky, which is the only place a night landscape shows it anyway.
   */
  ground: '#111c23',

  wall: '#1e2733',
  wallLit: '#26313e',
  wallDark: '#141b24',
  roof: '#0f151c',
  roofShade: '#0b1117',
  fascia: '#1a222c',
  wood: '#2a2620',
  woodDark: '#1a1712',
  woodLight: '#3a3227',
  boarded: '#242018',
  boardLine: '#181510',
  doorway: '#0c0f12',

  concrete: '#18232e',
  concreteDark: '#141e28',
  concreteDeep: '#111a23',
  glass: '#0d1720',
  glassDark: '#0a1219',
  pier: '#22303c',
  parapet: '#26343f',
  plantRoom: '#1b262f',
  mast: '#2b3a45',
  shopfront: '#0b141b',

  quayEdge: '#22303a',
  quayFace: '#111a20',
  silt: '#1c1e1a',
  siltFace: '#141712',
  reed: '#1b2318',
  water: '#081820',
  waterLine: '#123340',
  ripple: '#2e6b7d',

  stone: '#161f26',
  stoneLight: '#22303a',
  stoneParapet: '#1c2831',
  stoneBaluster: '#131c22',
  archShadow: '#050c11',
  steel: '#18242c',
  stay: '#2b3a44',
  pilePier: '#0e161c',

  lamp: '#26333c',
  lampHead: '#394a55',
  trunk: '#12181a',
  foliage: '#101a18',
  foliageLit: '#16241f',
  ford: '#1b2126',
  fordWet: '#4a6470',

  // the lit half of the palette
  warm: '#ffc477',
  warmDeep: '#e89a45',
  warmCore: '#fff0cf',
  brand: '#d9954a',
  neonCyan: '#2dd4bf',
  neonRed: '#ff4655',
  neonViolet: '#8c6fdc',
  moonFace: '#e8f1f6',
  moonHalo: '#cfe4ee',
  moonSea: '#cbd9e2',
  star: '#dcecf4',
  skyGlass: '#7fb3c9',
} as const;

/**
 * How far past the frame every full-bleed band is drawn.
 *
 * SVG clips to the viewport, not to the `viewBox`, so a band drawn this wide still fills a very wide
 * panel once `xMidYMax meet` has scaled the scene to its height. That is what lets the water, the
 * terraces and the bridge reach the panel's edges without a second, viewport-sized drawing.
 */
const FAR = 2600;

/**
 * How far *detail* is worth drawing, as opposed to a full-bleed band.
 *
 * The widest the 720×340 scene is ever cropped to is roughly −260…980, so a lamp, a baluster or a
 * lit window drawn at −2400 is never seen. In daylight that waste was a few hundred shapes; at night
 * the lit street furniture and the traffic multiply it, and the cost is paid on every change of step.
 */
const VIS_L = -460;
const VIS_R = 1180;

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
 * Last step the frontage table places a building at.
 *
 * The quay is full by then — the waterfront runs off the frame on both sides — so the twelfth step is
 * where the colony's *own* elevation stops growing. It is not where the drawing stops: the ladder is
 * open-ended, and past this the city behind keeps building instead (see {@link buildTownScene}).
 */
const LAST_FRONTAGE_STAGE = 11;

/**
 * How many further steps still change the skyline behind the colony.
 *
 * A run climbing at its calibrated pace of one step a week crosses about eleven; a squad clearing
 * nearly every challenge reaches efficiency twenty-one, which is step seventeen. Six covers exactly
 * the steps the ladder names past the citadel, so every named milestone still changes the picture;
 * past it the horizon is closed and there is nothing left to fill.
 */
const BEYOND_STAGES = 6;

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

const ellipse = (
  fill: string,
  cx: number,
  cy: number,
  rx: number,
  ry: number,
  opacity?: number,
): TownShape => ({ kind: 'ellipse', fill, cx, cy, rx, ry, opacity });

/**
 * A soft edge, stacked out of full-bleed bands.
 *
 * The scene is emitted as flat primitives on purpose, which means every surface meets the next one
 * on a hard line. That was invisible on a 19rem band and is not on a panel holding a screen: the
 * ground, the track and the water read as three black stripes laid over one another. Four or five
 * bands of falling opacity turn each of those lines into a transition, at a cost of five rects.
 *
 * @param colour - Colour to fade in.
 * @param y - Where the fade starts.
 * @param height - How far it runs before the surface itself takes over.
 * @param steps - How many bands to spend on it.
 * @param alpha - Opacity the last band reaches.
 * @returns The bands, top first.
 */
function feather(colour: string, y: number, height: number, steps: number, alpha = 1): TownShape[] {
  const slice = height / steps;

  // Squared rather than linear: a straight ramp opens on a step of its full strength divided by the
  // count, and against a flat ground that first step is the edge the fade exists to remove.
  return Array.from({ length: steps }, (_, index) =>
    band(
      colour,
      y + index * slice,
      slice + 0.5,
      Number((((index + 1) / steps) ** 2 * alpha).toFixed(3)),
    ),
  );
}

/**
 * A light source, drawn as three stacked ellipses rather than with a blur filter: the scene is
 * emitted as flat primitives and has to stay that way, and a hand-stacked halo is also the only
 * version that keeps its weight when the panel is scaled to a different height.
 *
 * Tight on purpose. The outer ring used to reach two and a half times the source and the core sat at
 * thirty per cent, which on a small panel was a lamp and on a large one was a coloured cloud with a
 * dot in it. A light at night has a bright centre and falls off fast; what is left over is the sky's
 * job, not the lamp's.
 */
function glow(colour: string, cx: number, cy: number, r: number, strength = 1): TownShape[] {
  return [
    ellipse(colour, cx, cy, r * 1.6, r * 1.6, 0.05 * strength),
    ellipse(colour, cx, cy, r * 1, r * 1, 0.1 * strength),
    ellipse(colour, cx, cy, r * 0.55, r * 0.55, 0.26 * strength),
  ];
}

/** A neon tube: the bright core, and the two washes it throws on the wall behind it. */
function neonBar(colour: string, x: number, y: number, w: number, h: number): TownShape[] {
  return [
    rect(colour, x - h * 2, y - h * 2, w + h * 4, h * 5, 0.1),
    rect(colour, x - h * 0.6, y - h * 0.6, w + h * 1.2, h * 2.2, 0.22),
    rect(colour, x, y, w, h),
  ];
}

/**
 * Deterministic pseudo-random sequence. Every scattered thing in the scene draws from one of these:
 * a skyline that reshuffles itself on every change detection pass is not a place.
 */
function seeded(seed: number): () => number {
  let state = Math.abs(Math.round(seed)) % 2147483647 || 7;

  return (): number => {
    state = (state * 1103515245 + 12345) % 2147483648;

    return state / 2147483648;
  };
}

/**
 * A glazed elevation at night: a dark pane, then a grid of cells, some of them lit.
 *
 * `life` is the population against its ceiling, and it is what decides how many cells are on. Warm
 * dominates on purpose — dwellings first, a minority of cold offices, one or two violet — so a tower
 * reads as lived in rather than as a server room.
 *
 * Each building draws its character before a single window: a skyline lit at one uniform rate is a
 * texture, not a city. What makes one read is that this block is an office still working, that one
 * is asleep, and the one between them is full of people home.
 */
function glazing(
  x: number,
  y: number,
  w: number,
  h: number,
  seed: number,
  life: number,
  cell = 4.6,
): TownShape[] {
  const random = seeded(seed);
  const mood = random();
  const office = mood < 0.3;
  const sleepy = mood > 0.86;
  const density = (0.1 + life * 0.72) * (sleepy ? 0.38 : 1);
  const cellW = cell * (office ? 1.25 : 1);
  const stepX = cellW + 2.2;
  const stepY = cell + 2.6;
  const cellH = cell * 0.72;
  const shapes: TownShape[] = [rect(C.glass, x, y, w, h)];

  for (let floorY = y + 2; floorY < y + h - cell; floorY += stepY) {
    // a floor lit end to end: a corridor, or a plateau nobody switched off
    if (random() < (office ? 0.22 : 0.09) * (0.35 + life)) {
      const colour = office ? C.neonCyan : C.warm;
      shapes.push(
        rect(colour, x, floorY - 1.6, w, cellH + 3.2, 0.1),
        rect(colour, x + 1.4, floorY, w - 2.8, cellH, 0.5),
      );
      continue;
    }

    for (let cellX = x + 1.6; cellX < x + w - cellW; cellX += stepX) {
      if (random() > density) {
        shapes.push(rect(C.glassDark, cellX, floorY, cellW, cellH, 0.9));
        continue;
      }

      const tone = random();
      const colour = office
        ? tone > 0.93
          ? C.neonViolet
          : tone > 0.42
            ? C.neonCyan
            : C.warm
        : tone > 0.96
          ? C.neonViolet
          : tone > 0.88
            ? C.neonCyan
            : C.warm;

      shapes.push(
        rect(colour, cellX - 0.8, floorY - 0.8, cellW + 1.6, cellH + 1.6, 0.14),
        rect(colour, cellX, floorY, cellW, cellH, 0.5 + random() * 0.5),
      );
    }
  }

  // structural mullions, which is what stops a lit elevation from reading as a grid of pixels
  for (let mullionX = x + stepX * 3; mullionX < x + w - 2; mullionX += stepX * 3) {
    shapes.push(rect(C.glassDark, mullionX - 1, y, 1.6, h, 0.55));
  }

  // the sky still lands on the glass, just barely
  shapes.push(
    poly(
      C.skyGlass,
      `${x},${y + h * 0.8} ${x + w},${y + h * 0.24} ${x + w},${y + h * 0.4} ${x},${y + h}`,
      0.05,
    ),
  );

  return shapes;
}

/** Lit interiors behind the ground-floor glass: what makes a street a street after dark. */
function shopfront(x: number, w: number, base: number, seed: number, life: number): TownShape[] {
  const random = seeded(seed + 91);
  const shapes: TownShape[] = [rect(C.shopfront, x + 3, base - 26, w - 6, 26)];

  for (let unitX = x + 5; unitX < x + w - 8; unitX += 13) {
    if (random() > 0.28 + (1 - life) * 0.4) {
      continue;
    }

    shapes.push(
      rect(C.warm, unitX, base - 22, 9, 15, 0.5),
      rect(C.warm, unitX - 3, base - 25, 15, 21, 0.09),
      ellipse(C.warm, unitX + 4.5, base + 1, 11, 4, 0.09),
    );
  }

  shapes.push(
    rect(C.parapet, x + 3, base - 27, w - 6, 1.6),
    rect(C.quayEdge, x - 2, base - 30, w + 4, 4),
  );

  return shapes;
}

/** Beacon on a mast: the only red in the scene, and the same red the threat wears below it. */
function mast(x: number, top: number, length: number): TownShape[] {
  return [
    line(C.mast, 2.4, x, top, x, top - length),
    line(C.mast, 1.6, x - 5, top - length * 0.55, x + 5, top - length * 0.55),
    ...glow(C.neonRed, x, top - length, 2.6, 1.15),
  ];
}

/** Parapet, plant room and, on some roofs, a neon strip: a roof that is maintained. */
function roofKit(x: number, w: number, top: number, lit: boolean): TownShape[] {
  const shapes: TownShape[] = [
    rect(C.parapet, x - 3, top - 4, w + 6, 4),
    rect(C.plantRoom, x + w * 0.1, top - 13, w * 0.28, 9),
  ];

  if (lit) {
    shapes.push(...neonBar(C.neonCyan, x + w * 0.44, top - 8, w * 0.48, 1.6));
  }

  return shapes;
}

/**
 * A framed board up on the roof, on legs.
 *
 * Bigger and rarer than the vertical strips: two or three of these carry the skyline's signage and
 * the rest of the neon is architecture. A city where every roof has a sign is a set, not a place.
 */
function roofSign(x: number, w: number, top: number, colour: string, seed: number): TownShape[] {
  const random = seeded(seed);
  const boardW = Math.min(w * 0.86, 34);
  const boardX = x + (w - boardW) / 2;
  const boardH = 10;
  const boardY = top - 20;
  const shapes: TownShape[] = [
    rect(C.mast, boardX + 1.5, boardY + boardH, 1.6, 10),
    rect(C.mast, boardX + boardW - 3, boardY + boardH, 1.6, 10),
    rect(C.glassDark, boardX, boardY, boardW, boardH),
    rect(colour, boardX - 3, boardY - 3, boardW + 6, boardH + 6, 0.09),
  ];

  for (let glyphX = boardX + 3; glyphX < boardX + boardW - 4; glyphX += 7) {
    shapes.push(rect(colour, glyphX, boardY + 2.6, 4.2, boardH - 5.2, 0.55 + random() * 0.45));
  }

  shapes.push(
    rect(colour, boardX, boardY, boardW, 1.3, 0.9),
    rect(colour, boardX, boardY + boardH - 1.3, boardW, 1.3, 0.9),
  );

  return shapes;
}

/** A vertical sign board, the tall stacked kind: three or four lit blocks down a narrow panel. */
function signBoard(x: number, top: number, h: number, colour: string, seed: number): TownShape[] {
  const random = seeded(seed);
  const w = 5.5;
  const shapes: TownShape[] = [rect(colour, x - 2.5, top - 2.5, w + 5, h + 5, 0.07)];

  for (let blockY = top + 2; blockY < top + h - 4; blockY += 8) {
    shapes.push(rect(colour, x + 1.2, blockY, w - 2.4, 4.6, 0.55 + random() * 0.45));
  }

  shapes.push(rect(colour, x, top, w, 1.4, 0.9));

  return shapes;
}

/**
 * The colony's first houses: rendered walls, a slate gable, a lit doorway, a window and a chimney.
 * Small, but built to stay — the settlement is founded, not camped.
 */
function house(x: number, w: number, h: number, life: number, flip = false): TownShape[] {
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
  const lit = seeded(x * 13)() < 0.25 + life * 0.75;

  const shapes: TownShape[] = [
    rect(C.wall, chimneyX, ridge + 4, chimneyW, wallH * 0.9),
    rect(C.wallDark, chimneyX - 1.5, ridge + 1, chimneyW + 3, 3.5),
    rect(C.wall, x, eave, w, wallH),
    rect(C.wallLit, x, eave, w * 0.34, wallH),
    rect(C.wallDark, x, base - 4, w, 4),
    poly(C.roof, `${x - 5},${eave + 4} ${cx},${ridge} ${x + w + 5},${eave + 4}`),
    poly(C.roofShade, `${cx},${ridge} ${x + w + 5},${eave + 4} ${cx},${eave + 4}`),
    rect(C.fascia, x - 5, eave + 4, w + 10, 3),
    rect(C.wood, doorX, base - doorH, doorW, doorH),
    rect(C.wallDark, doorX, base - doorH, doorW, 2),
    // a lamp over every door: the settlement is inhabited even at its smallest
    ...glow(C.warm, doorX + doorW / 2, base - doorH - 1, 3.2, 0.9),
    ellipse(C.warm, doorX + doorW / 2, base + 1, 11, 3.5, 0.1),
    rect(C.wallDark, winX - 2, winY - 2, winW + 4, winH + 4),
    rect(lit ? C.warm : C.glass, winX, winY, winW, winH, lit ? 0.86 : undefined),
  ];

  if (lit) {
    shapes.push(rect(C.warm, winX - 3, winY - 3, winW + 6, winH + 6, 0.12));
  }

  shapes.push(
    rect(C.wallDark, winX + winW / 2 - 0.8, winY, 1.6, winH, 0.8),
    rect(C.wallDark, winX, winY + winH / 2 - 0.8, winW, 1.6, 0.8),
  );

  return shapes;
}

/** Boarded walls under a single sloping roof, and one lamp: a store, not a dwelling. */
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
    ...glow(C.warmDeep, x + w * 0.5, base - h * 0.5, 2.6, 0.7),
  );

  return shapes;
}

/** Masonry base, one glazed band, a neon fascia over the shops: the step between houses and towers. */
function lowRise(x: number, w: number, h: number, life: number, board?: string): TownShape[] {
  const base = QUAY_Y - liftAt(x);
  const shapes: TownShape[] = [
    rect(C.wall, x, base - h, w, h),
    rect('#ffffff', x, base - h, w * 0.2, h, 0.03),
    ...glazing(x + w * 0.1, base - h * 0.82, w * 0.8, h * 0.3, x * 7 + 3, life, 4),
    ...shopfront(x, w, base, x, life),
    ...neonBar(C.neonCyan, x + 6, base - 31, w - 12, 1.4),
    ...roofKit(x, w, base - h, false),
  ];

  if (board) {
    shapes.push(...roofSign(x, w, base - h, board, x * 13 + 2));
  }

  return shapes;
}

function midRise(x: number, w: number, h: number, life: number, board?: string): TownShape[] {
  const base = QUAY_Y - liftAt(x);

  return [
    rect(C.concrete, x, base - h, w, h),
    ...glazing(x + 4, base - h + 6, w - 8, h - 40, x * 11 + 5, life),
    ...shopfront(x, w, base, x + 2, life),
    ...(board ? roofSign(x, w, base - h, board, x * 7 + 5) : []),
    ...roofKit(x, w, base - h, true),
  ];
}

/** A straight shaft, fully glazed on structural piers, capped and sometimes masted. */
function tower(
  x: number,
  w: number,
  h: number,
  life: number,
  options: { readonly sign?: boolean; readonly mast?: number } = {},
): TownShape[] {
  const base = QUAY_Y - liftAt(x);
  const shapes: TownShape[] = [
    rect(C.concreteDark, x, base - h, w, h),
    ...glazing(x + 3, base - h + 4, w - 6, h - 34, x * 17 + 9, life),
  ];

  for (let i = 1; i < 3; i++) {
    shapes.push(rect(C.pier, x + (w * i) / 3 - 1.2, base - h + 4, 2.4, h - 34, 0.55));
  }

  shapes.push(
    ...shopfront(x, w, base, x + 4, life),
    ...roofKit(x, w, base - h, options.sign ?? false),
  );

  if (options.sign) {
    shapes.push(...signBoard(x + 2, base - h + 20, h * 0.3, C.neonCyan, x * 5 + 2));
  }
  if (options.mast) {
    shapes.push(...mast(x + w / 2, base - h - 13, options.mast));
  }

  return shapes;
}

/** Two stacked volumes, the upper one narrower, with a violet crown on the setback itself. */
function setback(x: number, w: number, h: number, life: number, lit = false): TownShape[] {
  const base = QUAY_Y - liftAt(x);
  const upperW = w * 0.62;
  const upperX = x + (w - upperW) / 2;
  const lower = h * 0.5;

  return [
    rect(C.concreteDark, x, base - lower, w, lower),
    ...glazing(x + 3, base - lower + 4, w - 6, lower - 34, x * 19 + 1, life),
    rect(C.parapet, x - 3, base - lower - 4, w + 6, 4),
    ...neonBar(C.neonViolet, x - 1, base - lower - 6, w + 2, 1.6),
    rect(C.concreteDark, upperX, base - h, upperW, h - lower),
    ...glazing(upperX + 3, base - h + 4, upperW - 6, h - lower - 8, x * 23 + 6, life),
    ...shopfront(x, w, base, x + 6, life),
    ...roofKit(upperX, upperW, base - h, lit),
    ...mast(upperX + upperW / 2, base - h - 13, 26),
  ];
}

/** The landmark: the tallest shaft, lit corners, a crown band in the application's amber, a spire. */
function spire(x: number, w: number, h: number, life: number): TownShape[] {
  const base = QUAY_Y - liftAt(x);
  const cx = x + w / 2;

  return [
    poly(
      C.concreteDeep,
      `${x},${base} ${x + w},${base} ${x + w - 5},${base - h} ${x + 5},${base - h}`,
    ),
    ...glazing(x + 8, base - h + 6, w - 16, h - 46, x * 29 + 4, life),
    // the two light lines up the corners, which is what makes it read as one tall thing
    rect(C.brand, x + 5.5, base - h, 1.4, h - 12, 0.5),
    rect(C.brand, x + w - 6.9, base - h, 1.4, h - 12, 0.5),
    rect(C.parapet, x + 3, base - h - 7, w - 6, 7),
    ...neonBar(C.brand, x + 6, base - h - 5, w - 12, 3),
    ...glow(C.brand, cx, base - h - 4, 9, 1.1),
    poly(C.pier, `${cx - 5},${base - h - 7} ${cx + 5},${base - h - 7} ${cx},${base - h - 40}`),
    ...shopfront(x, w, base, x + 8, life),
    ...mast(cx, base - h - 40, 22),
  ];
}

/** A glazed link between two towers, lit underneath: the upper city reads as connected. */
function skybridge(x1: number, x2: number, up: number): TownShape[] {
  const base = QUAY_Y - liftAt(x1);

  return [
    rect(C.concreteDark, x1, base - up, x2 - x1, 11),
    rect(C.neonCyan, x1, base - up + 4, x2 - x1, 2.5, 0.75),
    rect(C.neonCyan, x1, base - up + 2, x2 - x1, 7, 0.12),
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
  readonly draw: (life: number) => readonly TownShape[];

  /** The sign on this building whose ballast is failing, if it is the one that carries it. */
  readonly flicker?: () => readonly TownShape[];
}

const FRONTAGE: readonly FrontageEntry[] = [
  { stage: 0, until: 3, x: 22, w: 30, draw: () => shed(22, 30, 26) },
  { stage: 0, until: 3, x: 58, w: 40, draw: (life) => house(58, 40, 38, life) },
  { stage: 0, until: 3, x: 104, w: 34, draw: (life) => house(104, 34, 31, life, true) },
  { stage: 0, until: 3, x: 144, w: 28, draw: () => shed(144, 28, 23, true) },
  { stage: 1, x: 176, w: 56, draw: (life) => lowRise(176, 56, 48, life, C.neonCyan) },
  { stage: 2, x: 234, w: 50, draw: (life) => lowRise(234, 50, 42, life) },
  {
    stage: 3,
    x: 302,
    w: 52,
    draw: (life) => midRise(302, 52, 78, life),
    flicker: () => signBoard(345, 184, 34, C.neonRed, 907),
  },
  { stage: 4, x: 356, w: 46, draw: (life) => midRise(356, 46, 64, life, C.neonRed) },
  { stage: 5, x: 404, w: 40, draw: (life) => tower(404, 40, 104, life, { sign: true }) },
  { stage: 6, x: 428, w: 36, draw: (life) => tower(428, 36, 88, life) },
  { stage: 7, x: 474, w: 50, draw: (life) => setback(474, 50, 138, life) },
  { stage: 8, x: 526, w: 38, draw: (life) => tower(526, 38, 118, life, { mast: 18, sign: true }) },
  { stage: 9, x: 566, w: 58, draw: (life) => spire(566, 58, 188, life) },
  { stage: 10, x: 634, w: 40, draw: (life) => tower(634, 40, 126, life, { mast: 22 }) },
  { stage: 10, x: 620, w: 18, draw: () => skybridge(620, 638, 96) },
  { stage: 11, x: 678, w: 44, draw: (life) => setback(678, 44, 140, life, true) },
  // by the last steps the waterfront runs off the frame on both sides
  { stage: 11, x: 2, w: 40, draw: (life) => tower(2, 40, 96, life, { mast: 16, sign: true }) },
];

/**
 * The rest of the city, held back by haze. At night distance is not a paler shape, it is a dimmer
 * light: the mass is barely darker than the sky and what is actually seen is a dust of windows
 * going back to the horizon.
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
  life: number,
  dust: number,
): TownShape[] {
  const shapes: TownShape[] = [];
  const random = seeded(seed);

  for (let x = VIS_L; x < VIS_R; x += step) {
    const w = minW + random() * rangeW;
    const h = minH + random() * rangeH;
    const top = QUAY_Y - lift - h;
    shapes.push(rect(fill, x, top, w, h, opacity));

    if (random() > 0.74) {
      shapes.push(rect(fill, x + w * 0.3, top - 13, w * 0.4, 13, opacity));
    }
    if (random() > 0.88) {
      shapes.push(ellipse(C.neonRed, x + w * 0.5, top - 2, 1.4, 1.4, 0.5));
    }

    for (let windowY = top + 4; windowY < QUAY_Y - lift - 4; windowY += 8) {
      for (let windowX = x + 2; windowX < x + w - 2; windowX += 6) {
        if (random() > 0.1 + life * 0.34) {
          continue;
        }
        shapes.push(rect(random() > 0.86 ? C.neonCyan : C.warm, windowX, windowY, 1.8, 1.8, dust));
      }
    }
  }

  return shapes;
}

/**
 * The ground: one silhouette, stepping down from the back of the scene to the quay.
 *
 * Drawn as a single filled outline rather than as three surfaces meeting on two lines. Each terrace
 * used to carry its own value and a lighter cap along its top edge, and both of those were read as
 * faults rather than as terrain: three blacks with two seams between them, and a bright lip running
 * along every slope with nothing above it to justify a lit edge. One colour and one outline leave
 * the relief to the only thing that can carry it at night, which is where the ground cuts the sky.
 *
 * @returns The ground, as one shape.
 */
function terraces(): TownShape[] {
  const run = 46;
  const last = TERRACES[TERRACES.length - 1];
  const points: string[] = [`${-FAR},${SCENE_H}`, `${-FAR},${QUAY_Y - TERRACES[0].lift}`];

  // Walked left to right: the flat of each terrace, then the bank down to the next one.
  TERRACES.forEach((step, index) => {
    const next = TERRACES[index + 1];
    if (next === undefined) {
      return;
    }

    points.push(`${step.until - run},${QUAY_Y - step.lift}`, `${step.until},${QUAY_Y - next.lift}`);
  });

  points.push(`${FAR},${QUAY_Y - last.lift}`, `${FAR},${SCENE_H}`);

  return [poly(C.ground, points.join(' '))];
}

/**
 * The camp's fire, before there is a street to light.
 *
 * Without it the first two steps are a dark field with four small roofs on it: in daylight the empty
 * sky over a camp was the point, at night an empty scene is only an empty scene. The fire is what
 * says somebody is out there, and it is what the ladder is worth climbing away from.
 */
function campfire(x: number): TownShape[] {
  const base = QUAY_Y - liftAt(x);

  return [
    // The pool it puts on the ground, kept to what a fire actually lights: it reached forty-six
    // units before, which on a full-height panel was a wash of amber the size of the camp.
    ellipse(C.warm, x, base, 24, 6, 0.06),
    ellipse(C.warm, x, base, 13, 3.5, 0.09),
    line(C.woodDark, 2.6, x - 7, base, x + 4, base - 8),
    line(C.woodDark, 2.6, x + 7, base, x - 4, base - 8),
    ...glow(C.warmDeep, x, base - 6, 7, 1.1),
    poly(C.warm, `${x},${base - 15} ${x + 4},${base - 2} ${x - 4},${base - 2}`, 0.8),
    poly(C.warmCore, `${x},${base - 9} ${x + 2},${base - 2} ${x - 2},${base - 2}`, 0.9),
  ];
}

/**
 * Street lamps and the pool each one puts on the ground — the scene's floor lighting, and what
 * arrives with the first streets rather than with the first houses.
 *
 * How many of them are actually burning is tonight's turnout. The post is always there, so the
 * street keeps its shape on a dead evening; only the light is missing, which is the difference
 * between a town that has stopped growing and a town where nobody came home. This is the one thing
 * in the scene that is different from yesterday on a day the ladder did not move, and the ladder
 * moves about once a week.
 *
 * @param stage - Frontage stage, which decides what furniture exists at all.
 * @param turnout - Share of the roster that played today, `0`–`1`.
 * @param from - Left edge of what the colony has built, which is where its own fence starts.
 * @param to - Right edge of the same.
 * @returns The furniture, in the frontage's own coordinates.
 */
function furnitureFor(stage: number, turnout: number, from: number, to: number): TownShape[] {
  const shapes: TownShape[] = [];
  // Deterministic per lamp, so the same evening always lights the same lamps: a street that
  // reshuffles which half of it is on at every change detection pass is not a street.
  const random = seeded(4211);

  if (stage < LAMP_STAGE) {
    shapes.push(...campfire(88));

    // A second fire is worth lighting only when there is a second shift to sit at it.
    if (turnout > 0.5) {
      shapes.push(...campfire(150));
    }
  }

  if (stage >= LAMP_STAGE) {
    for (let x = VIS_L + 20; x < VIS_R; x += 168) {
      const base = QUAY_Y - liftAt(x);
      shapes.push(
        rect(C.lamp, x, base - 42, 3, 44),
        { kind: 'arc', stroke: C.lamp, width: 3, d: `M${x + 1.5},${base - 42} q0,-9 12,-9` },
        rect(C.lampHead, x + 9, base - 52, 11, 3.5),
      );

      if (random() >= turnout) {
        continue;
      }

      shapes.push(
        ...glow(C.warm, x + 14.5, base - 49, 4.4, 0.9),
        // the cone it throws, and the pool it lands in
        poly(
          C.warm,
          `${x + 9},${base - 48} ${x + 20},${base - 48} ${x + 33},${base + 2} ${x - 4},${base + 2}`,
          0.05,
        ),
        ellipse(C.warm, x + 14.5, base + 1, 20, 5, 0.11),
      );
    }
  } else {
    /*
     * What a settlement has before it has streets: a lantern on a post, and nothing else.
     *
     * It used to carry a post-and-rail fence too. A fence is a repeating motif and the eye counts
     * it, which at this size made the whole width read as one hatched band across the camp; it also
     * enclosed the plot the colony is in the middle of building on, which is the opposite of what
     * the first steps of the ladder are about.
     */
    for (let x = from - 20; x < to + 20; x += 120) {
      const base = QUAY_Y - liftAt(x);

      // The head as well as the post: unlit, a bare upright is read as a stick left in the ground,
      // and on an evening nobody played every one of them is unlit.
      shapes.push(
        rect(C.woodDark, x, base - 26, 2.2, 26),
        rect(C.lampHead, x - 2, base - 29, 6.2, 3),
      );

      if (random() < turnout) {
        shapes.push(...glow(C.warmDeep, x + 1, base - 27, 3.4, 0.85));
      }
    }
  }

  for (let x = VIS_L + 96; x < VIS_R; x += 168) {
    const base = QUAY_Y - liftAt(x);
    shapes.push(
      rect(C.trunk, x - 2, base - 26, 4, 28),
      ellipse(C.foliage, x, base - 34, 13, 12),
      ellipse(C.foliageLit, x - 5, base - 40, 8, 7),
    );
  }

  return shapes;
}

/** Planks on driven piles, with lanterns: the first thing the colony builds over the water. */
function plankCrossing(): TownShape[] {
  const shapes: TownShape[] = [];

  for (let x = VIS_L + 30; x < VIS_R; x += 92) {
    shapes.push(
      rect(C.woodDark, x, DECK_Y + 5, 5, 44),
      rect(C.woodDark, x + 34, DECK_Y + 5, 5, 44),
      rect(C.woodDark, x, DECK_Y + 16, 39, 3),
    );
  }

  shapes.push(band(C.wood, DECK_Y, 7));

  for (let x = VIS_L; x < VIS_R; x += 13) {
    shapes.push(rect(C.woodDark, x, DECK_Y, 1.3, 7, 0.55));
  }

  shapes.push(band(C.woodLight, DECK_Y - 17, 2.8));

  for (let x = VIS_L + 18; x < VIS_R; x += 46) {
    shapes.push(rect(C.wood, x, DECK_Y - 17, 2.8, 17));
  }

  for (let x = VIS_L + 40; x < VIS_R; x += 138) {
    shapes.push(
      rect(C.woodDark, x, DECK_Y - 30, 2, 13),
      ...glow(C.warmDeep, x + 1, DECK_Y - 31, 4, 1),
    );
  }

  return shapes;
}

/**
 * Masonry piers, round arches, a parapet of balusters under a row of lamps. The crossing becomes
 * permanent at the same step the frontage does. The arch springs low enough that its crown stays
 * under the deck.
 */
function stoneCrossing(): TownShape[] {
  const span = 104;
  const radius = 32;
  const spring = SCENE_H - 8;
  const shapes: TownShape[] = [band(C.stone, DECK_Y, SCENE_H - DECK_Y)];

  for (let x = VIS_L + 52; x < VIS_R; x += span) {
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

  for (let x = VIS_L; x < VIS_R; x += 24) {
    shapes.push(rect(C.stoneBaluster, x, DECK_Y - 11, 5, 11, 0.7));
  }

  shapes.push(band(C.stoneLight, DECK_Y - 18, 4));

  for (let x = VIS_L + 26; x < VIS_R; x += 78) {
    shapes.push(
      rect(C.stoneBaluster, x, DECK_Y - 34, 2.4, 16),
      ...glow(C.warm, x + 1.2, DECK_Y - 35, 4.6, 1),
    );
  }

  return shapes;
}

/**
 * Traffic on the deck: headlights going one way, tail lights the other. Two rows of dots, and the
 * scene stops being an elevation and becomes an evening.
 *
 * @param y - Deck level to lay the two rows on.
 * @param turnout - Share of the roster that played today, `0`–`1`, which scales how busy the
 *   crossing is. A full evening jams it, a dead one leaves it to two cars.
 * @returns The traffic.
 */
function traffic(y: number, turnout: number): TownShape[] {
  const shapes: TownShape[] = [];
  const random = seeded(7717);
  // Never quite empty: a bridge with nothing at all on it reads as a rendering fault rather than as
  // a quiet night, and the colony has inhabitants even on a day the squad did not play.
  const density = 0.12 + turnout * 0.33;

  for (let x = VIS_L; x < VIS_R; x += 11) {
    if (random() > density) {
      continue;
    }
    shapes.push(rect(C.warm, x - 1, y - 1, 5.4, 3.4, 0.12), rect(C.warm, x, y, 3.4, 1.4, 0.9));
  }

  for (let x = VIS_L + 5; x < VIS_R; x += 13) {
    if (random() > density - 0.03) {
      continue;
    }
    shapes.push(
      rect(C.neonRed, x - 1, y + 2.4, 5, 3, 0.1),
      rect(C.neonRed, x, y + 3.2, 3, 1.3, 0.85),
    );
  }

  return shapes;
}

/** Cable-stayed: the one thing in the scene that is not a building. */
function cableCrossing(turnout: number): TownShape[] {
  const shapes: TownShape[] = [
    band(C.steel, DECK_Y, 7),
    band(C.quayEdge, DECK_Y - 3, 3),
    band(C.neonCyan, DECK_Y - 3.5, 1.2, 0.32),
    band(C.neonCyan, DECK_Y - 6, 6, 0.05),
    ...traffic(DECK_Y - 9, turnout),
  ];

  for (let x = VIS_L + 150; x < VIS_R; x += 470) {
    const top = DECK_Y - 52;
    shapes.push(
      rect(C.pilePier, x - 6, DECK_Y + 7, 12, 34),
      rect(C.quayEdge, x - 3, top, 6, DECK_Y - top),
      rect(C.quayEdge, x - 8, top + 19, 16, 3.5),
      ...glow(C.neonRed, x, top - 1, 3, 1.1),
    );

    for (let k = 1; k <= 4; k++) {
      const reach = k * 24;
      shapes.push(
        line(C.stay, 0.9, x, top + 3, x - reach, DECK_Y, 0.8),
        line(C.stay, 0.9, x, top + 3, x + reach, DECK_Y, 0.8),
        ellipse(C.neonCyan, x - reach * 0.55, top + 3 + (DECK_Y - top - 3) * 0.55, 1, 1, 0.7),
        ellipse(C.neonCyan, x + reach * 0.55, top + 3 + (DECK_Y - top - 3) * 0.55, 1, 1, 0.7),
      );
    }
  }

  return shapes;
}

/**
 * The moon: a face, and four steps of halo around it.
 *
 * The sky is the largest single surface in the picture and at the first steps it is nearly all there
 * is, so it needs a subject. Hand-stacked rather than blurred, like every other light in the scene.
 */
function moon(): TownShape[] {
  return [
    ellipse(C.moonHalo, 148, 48, 44, 44, 0.022),
    ellipse(C.moonHalo, 148, 48, 31, 31, 0.03),
    ellipse(C.moonHalo, 148, 48, 21, 21, 0.045),
    ellipse(C.moonHalo, 148, 48, 14.5, 14.5, 0.07),
    ellipse(C.moonFace, 148, 48, 11, 11, 0.92),
    ellipse(C.moonSea, 144, 45, 3, 2.4, 0.55),
    ellipse(C.moonSea, 151, 52, 2.2, 1.8, 0.5),
  ];
}

/**
 * The dome of light the city throws into its own sky, which is what a night city looks like from
 * outside it: the glow arrives before the buildings do. It grows with the ladder, not the population
 * — a camp throws none, and a citadel lights the whole horizon whether its people are home or not.
 */
function domeFor(stage: number): TownShape[] {
  if (stage < 2) {
    return [];
  }

  const strength = Math.min(1, (stage - 1) / 8);

  return [
    ellipse(C.warm, SCENE_W / 2, QUAY_Y - 10, 460, 150, 0.06 * strength),
    ellipse(C.warm, SCENE_W / 2, QUAY_Y + 6, 300, 90, 0.07 * strength),
    ellipse(C.neonCyan, SCENE_W * 0.72, QUAY_Y - 30, 190, 80, 0.05 * strength),
  ];
}

/**
 * The stars, drawn from one fixed sequence and cut short.
 *
 * The night is always starry; morale only decides how many get through. Cutting a fixed sequence
 * rather than reseeding means a falling morale puts the same stars out one by one instead of
 * reshuffling the sky, which is the difference between weather and a different night.
 *
 * @param clarity - Morale as `0`–`1`.
 * @returns Between sixteen and seventy-eight stars.
 */
export function starsFor(clarity: number): readonly TownShape[] {
  const shapes: TownShape[] = [];
  const random = seeded(4211);
  const count = Math.round(16 + Math.max(0, Math.min(1, clarity)) * 62);

  for (let i = 0; i < count; i++) {
    const x = random() * SCENE_W;
    const y = random() * 140;
    const r = 0.6 + random() * 0.6;
    shapes.push(ellipse(C.star, x, y, r, r, 0.35 + random() * 0.45));
  }

  return shapes;
}

/**
 * Builds the whole scene for one step of the ladder.
 *
 * Past {@link LAST_FRONTAGE_STAGE} the colony's own quay is full and growth moves behind it: each
 * further step raises and thickens the city on the horizon, so the last third of a strong run keeps
 * changing the picture instead of showing the same citadel for six weeks. That is also the truthful
 * reading — the squad is not rebuilding its waterfront a seventh time, the metropolis around it is
 * filling in.
 *
 * @param step - Ladder step the colony stands on, from zero, open-ended (see `tierStepFor`).
 * @param life - Population against its ceiling, `0`–`1`: how many windows are lit.
 * @returns Everything the template draws, in painting order.
 */
export function buildTownScene(step: number, life = 0.6, turnout = 1): TownScene {
  const stage = Math.max(0, Math.min(LAST_FRONTAGE_STAGE, step));
  const beyond = Math.max(0, Math.min(BEYOND_STAGES, step - LAST_FRONTAGE_STAGE));
  const lit = Math.max(0, Math.min(1, life));
  const busy = Math.max(0, Math.min(1, turnout));
  const visible = FRONTAGE.filter(
    (entry) => entry.stage <= stage && (entry.until === undefined || stage <= entry.until),
  );
  const rising = FRONTAGE.filter(
    (entry) => entry.stage === stage + 1 && (entry.until === undefined || stage + 1 <= entry.until),
  );

  // The frontage is centred on what it actually contains, so a hamlet is not left in the corner of
  // an avenue it has not built yet. The plot being built counts as part of it: centring on the
  // finished half alone would push what is going up off to one side of the frame.
  const placedAndRising = [...visible, ...rising];
  const minX = Math.min(...placedAndRising.map((entry) => entry.x));
  const maxX = Math.max(...placedAndRising.map((entry) => entry.x + entry.w));
  const frontageOffset = SCENE_W / 2 - (minX + maxX) / 2;

  /*
   * The rest of the city, twice veiled: each row keeps its mass, its windows are a dust, and a veil
   * is dropped in front of it so the colony's own frontage stays the only thing in focus.
   */
  const backdrop: TownShape[] = [];

  if (stage >= 4) {
    backdrop.push(
      ...hazeRow(38, 20, 15, 34, 66, C.hazeFar, 0.9, 7, 40, lit, 0.18),
      band(C.hazeVeil, QUAY_Y - 150, 160, 0.55),
    );
  }
  if (stage >= 6) {
    backdrop.push(
      ...hazeRow(54, 26, 17, 24, 54, C.hazeNear, 0.95, 31, 34, lit, 0.28),
      band(C.hazeVeil, QUAY_Y - 120, 130, 0.35),
    );
  }

  /*
   * The numbered citadels. Drawn first, so it stands furthest back and behind the colony's own
   * frontage: it rises step by step until it closes the sky the camp had all of, which is the
   * clearest thing a scene with a full quay can still say about growing.
   */
  if (beyond > 0) {
    backdrop.unshift(
      ...hazeRow(
        46,
        24,
        18,
        46 + beyond * 18,
        70 + beyond * 26,
        C.hazeFar,
        0.72,
        53,
        46,
        lit,
        0.14 + beyond * 0.02,
      ),
      band(C.hazeVeil, QUAY_Y - 240, 250, 0.46),
    );
  }

  const foreground: TownShape[] = [];
  const hasWater = stage >= WATER_STAGE;

  if (hasWater) {
    if (stage >= QUAY_STAGE) {
      foreground.push(
        band(C.quayEdge, QUAY_Y, 6),
        band(C.quayFace, QUAY_Y + 6, WATER_Y - QUAY_Y - 6),
        // The waterline is the hardest edge in the drawing and the one closest to the reader.
        ...feather(C.water, WATER_Y - 5, 5, 4, 0.7),
      );
    } else {
      // not built yet: silt and reeds down to the water
      foreground.push(band(C.silt, QUAY_Y, 5), band(C.siltFace, QUAY_Y + 5, WATER_Y - QUAY_Y - 5));

      for (let x = VIS_L; x < VIS_R; x += 17) {
        const h = 7 + ((x * 31) % 9);
        foreground.push(line(C.reed, 1.4, x, WATER_Y + 1, x + 2, WATER_Y - h, 0.7));
      }
    }

    foreground.push(band(C.water, WATER_Y, SCENE_H - WATER_Y), band(C.waterLine, WATER_Y, 3, 0.7));

    for (let i = 0; i < 8; i++) {
      foreground.push(
        band(C.ripple, WATER_Y + 5 + i * 8, 1.6, Number((0.14 - i * 0.015).toFixed(3))),
      );
    }
  }

  /*
   * Before the water is dug there is nothing here at all: the ground the terraces already drew runs
   * on to the bottom of the frame.
   *
   * There used to be a track across it — a slab of its own colour, forty units deep, edge to edge.
   * Whatever was stamped on it, a full-width band in a second colour under a single-colour ground is
   * read as a line, and the first step of the ladder does not need a road it has not built.
   */

  const placed: TownBuilding[] = visible.map((entry) => ({
    x: entry.x,
    w: entry.w,
    shapes: entry.draw(lit),
  }));

  /*
   * The site. Drawn unlit: a building with its windows on before it has walls is a building that is
   * finished, and the whole point of the site is that it is not. It lights up when it is paid for,
   * which is the step landing.
   */
  const construction = rising.flatMap((entry) => entry.draw(0));

  const crossing = crossingAt(stage);
  if (crossing === 'ford') {
    for (let x = VIS_L; x < VIS_R; x += 21) {
      foreground.push(
        ellipse(C.ford, x, DECK_Y + 22, 7, 3),
        ellipse(C.fordWet, x - 1, DECK_Y + 21, 4, 1.2, 0.4),
      );
    }
  } else if (crossing === 'plank') {
    foreground.push(...plankCrossing());
  } else if (crossing === 'stone') {
    foreground.push(...stoneCrossing());
  } else if (crossing === 'cable') {
    foreground.push(...cableCrossing(busy));
  }

  /*
   * The frame closes on the dark it opened on. Without it the last surface the scene draws — the
   * water, or the track — runs flat off the bottom edge and meets the strip under it on a line, and
   * a picture that ends on a line reads as a picture that was cut rather than as one that ends.
   */
  foreground.push(...feather(C.night, SCENE_H - 54, 54, 14, 0.62));

  return {
    viewBox: `0 0 ${SCENE_W} ${SCENE_H}`,
    moon: moon(),
    dome: domeFor(stage),
    backdrop,
    terraces: terraces(),
    buildings: placed,
    flicker: visible.flatMap((entry) => entry.flicker?.() ?? []),
    furniture: furnitureFor(stage, busy, minX, maxX),
    construction,
    foreground,
    frontageOffset,
  };
}

/**
 * What the frontage leaves on the water: a long column of light under each building, widening and
 * breaking up as it comes, cut by the ripples so it stays water.
 *
 * This is the reflection the daylight version could only hint at with a pale slab, and it is most of
 * what makes the scene read as a city at night rather than as a black skyline.
 *
 * @param buildings - The placed frontage.
 * @returns The reflections, in the frontage's own coordinates.
 */
export function reflectionsFor(buildings: readonly TownBuilding[]): readonly TownShape[] {
  return buildings.flatMap((building, index) => {
    const cx = building.x + building.w / 2;
    const random = seeded(building.x * 3 + 11);
    const shapes: TownShape[] = [
      rect(C.warm, building.x + building.w * 0.16, WATER_Y, building.w * 0.68, 46, 0.05),
      rect(C.warm, cx - building.w * 0.14, WATER_Y, building.w * 0.28, 52, 0.08),
    ];

    for (let i = 0; i < 13; i++) {
      const spread = building.w * (0.16 + i * 0.05);
      const jitter = (random() - 0.5) * building.w * 0.14;
      shapes.push(
        rect(
          i % 4 === 2 ? C.neonCyan : C.warm,
          cx - spread / 2 + jitter,
          WATER_Y + 1 + i * 3.4,
          spread,
          1.7,
          Math.max(0.05, Number((0.48 - i * 0.034).toFixed(3))),
        ),
      );
    }

    // one long red thread from the beacons, so the water carries all three of the scene's colours
    if (index % 3 === 1) {
      shapes.push(rect(C.neonRed, cx - 1.6, WATER_Y, 3.2, 26, 0.2));
    }

    return shapes;
  });
}

/** Whether the scene has any water at this step, which is what the reflections are drawn on. */
export function hasWaterAt(step: number): boolean {
  return step >= WATER_STAGE;
}

/**
 * The rocket, part by part.
 *
 * Ten states, and each guardian defeated adds a real part: the rocket of state ten is not the one
 * of state one scaled up. Shared by the base scene of the overview and the blueprint of the
 * campaign page, so the two pages draw the same ship.
 *
 * Pure DOM construction with no Angular dependency. The frame is the caller's: the drawing stands
 * on `y = 0` and builds upward, so it is placed under a `scale(1 -1)` transform.
 */

const NS = 'http://www.w3.org/2000/svg';

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

const C = ROCKET_PALETTE;

/**
 * Number of parts the finished launcher has: one per guardian of the campaign.
 */
export const ROCKET_PART_COUNT = 10;

type Attrs = Readonly<Record<string, string | number>>;

/**
 * Creates one SVG element with its attributes set.
 */
export function svgElement<K extends keyof SVGElementTagNameMap>(
  name: K,
  attrs: Attrs = {},
): SVGElementTagNameMap[K] {
  const node = document.createElementNS(NS, name);
  for (const [key, value] of Object.entries(attrs)) {
    node.setAttribute(key, String(value));
  }
  return node;
}

const el = svgElement;

/**
 * One stage of the rocket: half-width and height of the hull, fins, booster height, nose shape,
 * engines, gantry, portholes and marking bands.
 *
 * Ten states, and each guardian defeated adds a real part: the rocket of state ten is not the one
 * of state one scaled up.
 */
export interface ShipStage {
  readonly w: number;
  readonly h: number;
  readonly fins: number;
  readonly boost: number;
  readonly nose: 'none' | 'dome' | 'cone' | 'capsule';
  readonly eng: number;
  readonly gantry: number;
  readonly ports: number;
  readonly bands: number;
}

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

export function noseHeight(stage: ShipStage): number {
  if (stage.nose === 'dome') {
    return stage.w * 0.8;
  }
  return stage.nose === 'cone' ? stage.w * 2.4 : stage.w * 2.1;
}

export function shipHalf(stage: ShipStage): number {
  const boosterEdge = stage.boost ? stage.w + stage.w * 0.42 * 2 : 0;
  const finEdge = stage.fins ? stage.w * 1.9 : stage.w;
  return Math.max(boosterEdge, finEdge, stage.w);
}

/**
 * Outer profile alone, for the dotted template of what remains to be built.
 */
export function outline(stage: ShipStage): string {
  const top = SKIRT + stage.h;
  const nh = noseHeight(stage);
  const w = stage.w;
  const parts = [
    `M${-w} ${SKIRT} L${-w * 1.1} 0 L${w * 1.1} 0 L${w} ${SKIRT} L${w} ${top}` +
      (stage.nose === 'dome'
        ? ` Q${w} ${top + nh} 0 ${top + nh} Q${-w} ${top + nh} ${-w} ${top}`
        : ` L0 ${top + nh} L${-w} ${top}`) +
      ' Z',
  ];
  if (stage.fins) {
    const fh = Math.max(14, stage.h * 0.26);
    parts.push(`M${-w} ${SKIRT} L${-w * 1.9} ${SKIRT - 2} L${-w} ${SKIRT + fh} Z`);
    parts.push(`M${w} ${SKIRT} L${w * 1.9} ${SKIRT - 2} L${w} ${SKIRT + fh} Z`);
  }
  if (stage.boost) {
    const bw = w * 0.42;
    for (const dir of [-1, 1]) {
      const cx = dir * (w + bw);
      parts.push(
        `M${cx - bw} 4 L${cx - bw} ${4 + stage.boost} L${cx} ${4 + stage.boost + bw * 2}` +
          ` L${cx + bw} ${4 + stage.boost} L${cx + bw} 4 Z`,
      );
    }
  }
  if (stage.nose === 'capsule') {
    const capBase = top + nh;
    parts.push(
      `M-1.6 ${capBase} L-1.6 ${capBase + w * 2.2} L1.6 ${capBase + w * 2.2} L1.6 ${capBase} Z`,
    );
  }
  return parts.join(' ');
}

export function animate(values: string, dur: string, begin?: string): SVGAnimateElement {
  return el('animate', {
    attributeName: 'opacity',
    values,
    dur,
    repeatCount: 'indefinite',
    ...(begin === undefined ? {} : { begin }),
  });
}

/**
 * The complete drawing, part by part.
 */
export function drawShip(stageIndex: number): SVGGElement {
  const stage = SHIP[stageIndex];
  const g = el('g');
  if (!stageIndex) {
    return g;
  }

  const top = SKIRT + stage.h;
  const nh = noseHeight(stage);
  const w = stage.w;
  const bw = w * 0.42;

  // Service gantry on one side only: two masts either side make a cage the rocket vanishes into.
  if (stage.gantry) {
    const gx = -(shipHalf(stage) + 22);
    const gh = stage.h * (stage.gantry === 2 ? 0.86 : 0.7);
    g.append(el('rect', { x: gx - 5, y: 0, width: 4, height: gh, fill: C.mast }));
    g.append(el('rect', { x: gx + 9, y: 0, width: 4, height: gh, fill: C.mast }));
    for (let y = 10; y < gh; y += 18) {
      g.append(el('rect', { x: gx - 5, y, width: 18, height: 1.4, fill: C.mast, opacity: 0.7 }));
    }
    const arms = stage.gantry === 2 ? [26, 74, 122, 170] : [26, 78];
    for (const ay of arms) {
      if (ay > gh) {
        continue;
      }
      g.append(
        el('rect', {
          x: gx + 13,
          y: ay,
          width: -gx - shipHalf(stage) - 5,
          height: 2.6,
          fill: C.mast,
        }),
      );
    }
    const beacon = el('circle', { cx: gx + 4, cy: gh + 4, r: 2.6, fill: C.red });
    beacon.append(animate('1;0.15;1', '2.6s'));
    g.append(beacon);
  }

  // Strap-on boosters, behind the hull.
  if (stage.boost) {
    for (const dir of [-1, 1]) {
      const cx = dir * (w + bw);
      g.append(
        el('rect', {
          x: cx - bw,
          y: 4,
          width: bw * 2,
          height: stage.boost,
          fill: C.steelDark,
          stroke: C.steelLit,
          'stroke-width': 1,
        }),
      );
      g.append(
        el('path', {
          d: `M${cx - bw} ${4 + stage.boost} L${cx} ${4 + stage.boost + bw * 2.1} L${cx + bw} ${4 + stage.boost} Z`,
          fill: C.steel,
          stroke: C.cyan,
          'stroke-width': 1,
          'stroke-opacity': 0.5,
        }),
      );
      g.append(el('rect', { x: cx - bw, y: 2, width: bw * 2, height: 5, fill: C.steelLit }));
    }
  }

  // Fins.
  if (stage.fins) {
    const fh = Math.max(14, stage.h * 0.26);
    for (const dir of [-1, 1]) {
      g.append(
        el('path', {
          d: `M${dir * w} ${SKIRT} L${dir * w * 1.9} ${SKIRT - 2} L${dir * w} ${SKIRT + fh} Z`,
          fill: C.steel,
          stroke: C.cyan,
          'stroke-width': 1,
          'stroke-opacity': 0.45,
        }),
      );
    }
  }

  // Skirt and engines.
  g.append(
    el('path', {
      d: `M${-w} ${SKIRT} L${-w * 1.12} 0 L${w * 1.12} 0 L${w} ${SKIRT} Z`,
      fill: C.steelDark,
      stroke: C.steelLit,
      'stroke-width': 1,
    }),
  );
  const spread = stage.eng === 1 ? [0] : [-w * 0.55, 0, w * 0.55];
  for (const ex of spread) {
    const r = stage.eng === 1 ? w * 0.5 : w * 0.3;
    g.append(
      el('path', {
        d: `M${ex - r * 0.6} 11 L${ex - r} 1 L${ex + r} 1 L${ex + r * 0.6} 11 Z`,
        fill: '#0c141c',
        stroke: C.steelLit,
        'stroke-width': 0.8,
      }),
    );
  }

  // The hull in rings, one per guardian defeated, the seam between them visible.
  g.append(
    el('rect', {
      x: -w,
      y: SKIRT,
      width: w * 2,
      height: stage.h,
      fill: C.steel,
      stroke: C.cyan,
      'stroke-width': 1.5,
    }),
  );
  g.append(
    el('rect', {
      x: -w,
      y: SKIRT,
      width: w * 0.42,
      height: stage.h,
      fill: '#ffffff',
      opacity: 0.06,
    }),
  );
  for (let ring = 1; ring < stageIndex; ring++) {
    const y = SKIRT + (stage.h / stageIndex) * ring;
    g.append(el('rect', { x: -w, y: y - 1.5, width: w * 2, height: 3, fill: C.steelLit }));
  }

  // Marking bands, from the sixth stage.
  if (stage.bands) {
    g.append(
      el('rect', {
        x: -w,
        y: SKIRT + stage.h * 0.62,
        width: w * 2,
        height: 6,
        fill: C.brand,
        opacity: 0.85,
      }),
    );
    g.append(
      el('rect', {
        x: -w,
        y: SKIRT + stage.h * 0.2,
        width: w * 2,
        height: 3,
        fill: C.warmCore,
        opacity: 0.55,
      }),
    );
  }

  // Portholes: life aboard, amber like everywhere else.
  if (stage.ports) {
    const rows = stage.ports * 2;
    for (let r = 0; r < rows; r++) {
      const y = SKIRT + stage.h * (0.3 + (r / rows) * 0.6);
      const port = el('circle', { cx: 0, cy: y, r: Math.max(1.6, w * 0.16), fill: C.warm });
      port.append(animate('0.95;0.55;0.95', `${(3 + r * 0.4).toFixed(1)}s`));
      g.append(port);
    }
  }

  // The nose.
  if (stage.nose === 'dome') {
    g.append(
      el('path', {
        d: `M${-w} ${top} Q${-w} ${top + nh} 0 ${top + nh} Q${w} ${top + nh} ${w} ${top} Z`,
        fill: C.steelLit,
      }),
    );
  } else {
    g.append(
      el('path', {
        d: `M${-w} ${top} L0 ${top + nh} L${w} ${top} Z`,
        fill: C.steelLit,
        stroke: C.cyan,
        'stroke-width': 1.2,
      }),
    );
  }

  // Capsule and escape tower, the last two parts fitted.
  if (stage.nose === 'capsule') {
    const capBase = top + nh;
    g.append(el('rect', { x: -2, y: capBase, width: 4, height: w * 1.6, fill: C.mast }));
    g.append(
      el('path', {
        d: `M-5 ${capBase + w * 1.6} L0 ${capBase + w * 2.2} L5 ${capBase + w * 1.6} Z`,
        fill: C.brand,
      }),
    );
  }

  return g;
}

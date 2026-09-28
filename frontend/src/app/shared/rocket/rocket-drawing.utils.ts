import { svgElement } from '@core/svg/svg-element.utils';
import { ROCKET_PALETTE, SHIP, SKIRT } from './rocket-drawing.constants';
import { ShipStage } from './rocket-drawing.model';

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

/**
 * Height of the nose for a stage, from its shape and hull width.
 */
export function noseHeight(stage: ShipStage): number {
  if (stage.nose === 'dome') {
    return stage.w * 0.8;
  }
  return stage.nose === 'cone' ? stage.w * 2.4 : stage.w * 2.1;
}

/**
 * Half-width of the ship at its widest, boosters and fins included.
 */
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

/**
 * Opacity animation node, looping forever.
 */
export function animate(values: string, dur: string, begin?: string): SVGAnimateElement {
  return svgElement('animate', {
    attributeName: 'opacity',
    values,
    dur,
    repeatCount: 'indefinite',
    ...(begin === undefined ? {} : { begin }),
  });
}

/**
 * The complete drawing, part by part.
 *
 * A painted launcher rather than a steel silhouette: an ivory hull shaded in flat bands so it reads
 * as a cylinder, the roll pattern of the great launchers at its foot, an amber tip. It has to
 * stand out against a night sky and a day sky alike.
 */
export function drawShip(stageIndex: number): SVGGElement {
  const stage = SHIP[stageIndex];
  const g = svgElement('g');
  if (!stageIndex) {
    return g;
  }

  const top = SKIRT + stage.h;
  const nh = noseHeight(stage);
  const w = stage.w;

  if (stage.gantry) {
    appendGantry(g, stage);
  }
  if (stage.boost) {
    for (const dir of [-1, 1]) {
      appendBooster(g, dir * (w + w * 0.42), w * 0.42, stage.boost);
    }
  }

  // Fins in ink, their outer edge in amber.
  if (stage.fins) {
    const fh = Math.max(14, stage.h * 0.26);
    for (const dir of [-1, 1]) {
      g.append(
        svgElement('path', {
          d: `M${dir * w} ${SKIRT} L${dir * w * 1.9} ${SKIRT - 2} L${dir * w} ${SKIRT + fh} Z`,
          fill: ROCKET_PALETTE.ink,
        }),
        svgElement('path', {
          d: `M${dir * w * 1.9} ${SKIRT - 2} L${dir * w} ${SKIRT + fh}`,
          stroke: ROCKET_PALETTE.brand,
          'stroke-width': 1.6,
        }),
      );
    }
  }

  // Skirt and engine bells.
  g.append(
    svgElement('path', {
      d: `M${-w} ${SKIRT} L${-w * 1.12} 0 L${w * 1.12} 0 L${w} ${SKIRT} Z`,
      fill: ROCKET_PALETTE.steelDark,
      stroke: ROCKET_PALETTE.steelLit,
      'stroke-width': 1,
    }),
  );
  const spread = stage.eng === 1 ? [0] : [-w * 0.55, 0, w * 0.55];
  for (const ex of spread) {
    const r = stage.eng === 1 ? w * 0.5 : w * 0.3;
    g.append(
      svgElement('path', {
        d: `M${ex - r * 0.6} 11 L${ex - r} 1 L${ex + r} 1 L${ex + r * 0.6} 11 Z`,
        fill: ROCKET_PALETTE.nozzle,
        stroke: ROCKET_PALETTE.steelLit,
        'stroke-width': 0.8,
      }),
    );
  }

  // The hull, one ring per guardian defeated, the seams visible.
  g.append(
    svgElement('rect', {
      x: -w,
      y: SKIRT,
      width: w * 2,
      height: stage.h,
      fill: ROCKET_PALETTE.hull,
    }),
  );

  // Roll pattern at the foot: black and white quarters, the mark of the great launchers.
  if (stage.fins) {
    const cellH = Math.min(9, stage.h * 0.07);
    for (let row = 0; row < 2; row++) {
      for (let col = 0; col < 4; col++) {
        if ((row + col) % 2 === 0) {
          g.append(
            svgElement('rect', {
              x: -w + (col * w) / 2,
              y: SKIRT + 3 + row * cellH,
              width: w / 2,
              height: cellH,
              fill: ROCKET_PALETTE.ink,
            }),
          );
        }
      }
    }
  }
  for (let ring = 1; ring < stageIndex; ring++) {
    const y = SKIRT + (stage.h / stageIndex) * ring;
    g.append(
      svgElement('rect', {
        x: -w,
        y: y - 0.7,
        width: w * 2,
        height: 1.4,
        fill: ROCKET_PALETTE.ink,
        opacity: 0.45,
      }),
    );
  }

  // Livery, from the sixth stage: an ink interstage and the amber band of the base.
  if (stage.bands) {
    g.append(
      svgElement('rect', {
        x: -w,
        y: SKIRT + stage.h * 0.34,
        width: w * 2,
        height: 6,
        fill: ROCKET_PALETTE.ink,
      }),
      svgElement('rect', {
        x: -w,
        y: SKIRT + stage.h * 0.62,
        width: w * 2,
        height: 8,
        fill: ROCKET_PALETTE.brand,
      }),
    );
  }

  // Portholes: life aboard, amber like everywhere else.
  if (stage.ports) {
    const rows = stage.ports * 2;
    for (let r = 0; r < rows; r++) {
      const y = SKIRT + stage.h * (0.3 + (r / rows) * 0.6);
      const port = svgElement('circle', {
        cx: -w * 0.2,
        cy: y,
        r: Math.max(1.4, w * 0.11),
        fill: ROCKET_PALETTE.warm,
        stroke: ROCKET_PALETTE.ink,
        'stroke-width': 0.8,
      });
      port.append(animate('0.95;0.6;0.95', `${(3 + r * 0.4).toFixed(1)}s`));
      g.append(port);
    }
  }
  appendShade(g, -w, w, SKIRT, top);

  // The nose, its tip in amber, shaded like the hull.
  if (stage.nose === 'dome') {
    g.append(
      svgElement('path', {
        d: `M${-w} ${top} Q${-w} ${top + nh} 0 ${top + nh} Q${w} ${top + nh} ${w} ${top} Z`,
        fill: ROCKET_PALETTE.brand,
      }),
      svgElement('path', {
        d: `M0 ${top} L0 ${top + nh} Q${w} ${top + nh} ${w} ${top} Z`,
        fill: ROCKET_PALETTE.shade,
        opacity: 0.28,
      }),
    );
  } else {
    const tip = top + nh * 0.62;
    g.append(
      svgElement('path', {
        d: `M${-w} ${top} L0 ${top + nh} L${w} ${top} Z`,
        fill: ROCKET_PALETTE.hull,
      }),
      svgElement('path', {
        d: `M${-w * 0.38} ${tip} L0 ${top + nh} L${w * 0.38} ${tip} Z`,
        fill: ROCKET_PALETTE.brand,
      }),
      svgElement('path', {
        d: `M0 ${top} L0 ${top + nh} L${w} ${top} Z`,
        fill: ROCKET_PALETTE.shade,
        opacity: 0.3,
      }),
    );
  }

  // Escape tower, the last part fitted: a lattice mast and its motor.
  if (stage.nose === 'capsule') {
    const base = top + nh;
    const mast = w * 1.6;
    const rails = `M-3 ${base} L-1.2 ${base + mast} M3 ${base} L1.2 ${base + mast}`;
    let braces = '';
    for (let k = 0; k < 4; k++) {
      const y0 = base + (mast / 4) * k;
      const y1 = y0 + mast / 4;
      braces += ` M${-3 + 0.45 * k} ${y0} L${2.55 - 0.45 * k} ${y1}`;
    }
    g.append(
      svgElement('path', {
        d: rails + braces,
        stroke: ROCKET_PALETTE.ink,
        'stroke-width': 1.1,
        fill: 'none',
      }),
      svgElement('rect', {
        x: -2,
        y: base + mast,
        width: 4,
        height: w * 0.3,
        fill: ROCKET_PALETTE.ink,
      }),
      svgElement('path', {
        d: `M-2 ${base + mast + w * 0.3} L0 ${base + w * 2.2} L2 ${base + mast + w * 0.3} Z`,
        fill: ROCKET_PALETTE.brand,
      }),
    );
  }

  return g;
}

/**
 * Flat cylinder shading over a vertical body: a lit strip on the left, two shade steps on the right.
 */
function appendShade(g: SVGGElement, x0: number, x1: number, y0: number, y1: number): void {
  const w = x1 - x0;
  const h = y1 - y0;
  g.append(
    svgElement('rect', {
      x: x0 + w * 0.14,
      y: y0,
      width: w * 0.14,
      height: h,
      fill: ROCKET_PALETTE.hullLit,
      opacity: 0.35,
    }),
    svgElement('rect', {
      x: x0 + w * 0.6,
      y: y0,
      width: w * 0.4,
      height: h,
      fill: ROCKET_PALETTE.shade,
      opacity: 0.2,
    }),
    svgElement('rect', {
      x: x0 + w * 0.84,
      y: y0,
      width: w * 0.16,
      height: h,
      fill: ROCKET_PALETTE.shade,
      opacity: 0.22,
    }),
  );
}

/**
 * A strap-on booster, behind the hull: ivory body, ink foot, amber cap.
 */
function appendBooster(g: SVGGElement, cx: number, bw: number, height: number): void {
  g.append(
    svgElement('rect', { x: cx - bw, y: 4, width: bw * 2, height, fill: ROCKET_PALETTE.hull }),
    svgElement('rect', {
      x: cx - bw,
      y: 4,
      width: bw * 2,
      height: Math.min(10, height * 0.1),
      fill: ROCKET_PALETTE.ink,
    }),
    svgElement('path', {
      d: `M${cx - bw} ${4 + height} L${cx} ${4 + height + bw * 2.1} L${cx + bw} ${4 + height} Z`,
      fill: ROCKET_PALETTE.brand,
    }),
    svgElement('rect', { x: cx - bw, y: 0, width: bw * 2, height: 4, fill: ROCKET_PALETTE.nozzle }),
  );
  appendShade(g, cx - bw, cx + bw, 4, 4 + height);
}

/**
 * Service gantry on one side only: two masts either side make a cage the rocket vanishes into.
 * A lattice tower with cross braces, arms reaching the hull, and a hammerhead jib once complete.
 */
function appendGantry(g: SVGGElement, stage: ShipStage): void {
  const half = shipHalf(stage);
  const gx = -(half + 22);
  const gh = stage.h * (stage.gantry === 2 ? 0.86 : 0.7);
  const left = gx - 5;
  const right = gx + 11;
  let lattice = `M${left} 0 V${gh} M${right} 0 V${gh}`;
  for (let y = 0; y + 14 <= gh; y += 14) {
    lattice += ` M${left} ${y} H${right} M${left} ${y} L${right} ${y + 14} M${right} ${y} L${left} ${y + 14}`;
  }
  g.append(
    svgElement('path', {
      d: lattice,
      stroke: ROCKET_PALETTE.mast,
      'stroke-width': 1.3,
      fill: 'none',
    }),
  );
  g.append(
    svgElement('rect', { x: left - 1, y: 0, width: 2.5, height: gh, fill: ROCKET_PALETTE.mast }),
    svgElement('rect', { x: right - 1.5, y: 0, width: 2.5, height: gh, fill: ROCKET_PALETTE.mast }),
  );

  const arms = stage.gantry === 2 ? [26, 74, 122, 170] : [26, 78];
  for (const ay of arms) {
    if (ay > gh) {
      continue;
    }
    g.append(
      svgElement('rect', {
        x: right,
        y: ay,
        width: -right - stage.w,
        height: 2.6,
        fill: ROCKET_PALETTE.mast,
      }),
    );
  }

  // The jib reaches over the rocket; its counterweight hangs on the far side.
  if (stage.gantry === 2) {
    g.append(
      svgElement('rect', {
        x: left - 16,
        y: gh + 4,
        width: -left + 16 - stage.w * 0.4,
        height: 3,
        fill: ROCKET_PALETTE.mast,
      }),
      svgElement('rect', {
        x: left - 16,
        y: gh - 4,
        width: 9,
        height: 8,
        fill: ROCKET_PALETTE.mast,
      }),
      svgElement('rect', { x: gx + 1, y: gh, width: 3, height: 12, fill: ROCKET_PALETTE.mast }),
    );
  }

  const beacon = svgElement('circle', {
    cx: gx + 3,
    cy: gh + (stage.gantry === 2 ? 14 : 4),
    r: 2.6,
    fill: ROCKET_PALETTE.red,
  });
  beacon.append(animate('1;0.15;1', '2.6s'));
  g.append(beacon);
}

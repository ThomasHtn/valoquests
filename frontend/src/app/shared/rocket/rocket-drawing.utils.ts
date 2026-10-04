import { svgElement } from '@core/svg/svg-element.utils';
import {
  GANTRY_ARM_HEIGHTS,
  GANTRY_BRACE_STEP,
  GANTRY_GAP,
  ROCKET_PALETTE,
  SHIP,
  SKIRT,
} from './rocket-drawing.constants';
import { ShipStage } from './rocket-drawing.model';

/**
 * Drawn upward from `y = 0`: the caller places it under a `scale(1 -1)` transform.
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
  const parts = [hullOutline(stage)];
  if (stage.fins) {
    parts.push(finOutline(stage, -1), finOutline(stage, 1));
  }
  if (stage.boost) {
    parts.push(boosterOutline(stage, -1), boosterOutline(stage, 1));
  }
  if (stage.nose === 'capsule') {
    parts.push(towerOutline(stage));
  }
  return parts.join(' ');
}

/**
 * Skirt, hull sides and nose as one closed path.
 */
function hullOutline(stage: ShipStage): string {
  const top = hullTop(stage);
  const tip = top + noseHeight(stage);
  const w = stage.w;
  const body = `M${-w} ${SKIRT} L${-w * 1.1} 0 L${w * 1.1} 0 L${w} ${SKIRT} L${w} ${top}`;
  const nose =
    stage.nose === 'dome'
      ? ` Q${w} ${tip} 0 ${tip} Q${-w} ${tip} ${-w} ${top}`
      : ` L0 ${tip} L${-w} ${top}`;
  return `${body}${nose} Z`;
}

/**
 * One fin, on the left (`dir = -1`) or right (`dir = 1`) side.
 */
function finOutline(stage: ShipStage, dir: number): string {
  const root = dir * stage.w;
  const tip = dir * stage.w * 1.9;
  return `M${root} ${SKIRT} L${tip} ${SKIRT - 2} L${root} ${SKIRT + finHeight(stage)} Z`;
}

/**
 * One booster with its pointed cap, on the left (`dir = -1`) or right (`dir = 1`) side.
 */
function boosterOutline(stage: ShipStage, dir: number): string {
  const bw = stage.w * 0.42;
  const cx = dir * (stage.w + bw);
  const capBase = 4 + stage.boost;
  return (
    `M${cx - bw} 4 L${cx - bw} ${capBase} L${cx} ${capBase + bw * 2}` +
    ` L${cx + bw} ${capBase} L${cx + bw} 4 Z`
  );
}

/**
 * The escape tower above the capsule, as a thin rectangle.
 */
function towerOutline(stage: ShipStage): string {
  const base = noseTip(stage);
  const towerTop = base + stage.w * 2.2;
  return `M-1.6 ${base} L-1.6 ${towerTop} L1.6 ${towerTop} L1.6 ${base} Z`;
}

/**
 * Altitude where the hull ends and the nose starts.
 */
function hullTop(stage: ShipStage): number {
  return SKIRT + stage.h;
}

/**
 * Altitude of the tip of the nose.
 */
function noseTip(stage: ShipStage): number {
  return hullTop(stage) + noseHeight(stage);
}

/**
 * Height of a fin along the hull, never too short to read on a small ship.
 */
function finHeight(stage: ShipStage): number {
  return Math.max(14, stage.h * 0.26);
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
 * Complete ship of a stage, painted ivory and amber to stand out on night and day skies.
 */
export function drawShip(stageIndex: number): SVGGElement {
  const stage = SHIP[stageIndex];
  const g = svgElement('g');
  if (!stageIndex) {
    return g;
  }

  // Back to front: what stands behind the hull is drawn first.
  if (stage.gantry) {
    appendGantry(g, stage);
  }
  if (stage.boost) {
    appendBoosters(g, stage);
  }
  if (stage.fins) {
    appendFins(g, stage);
  }
  appendSkirt(g, stage);
  appendHull(g, stage, stageIndex);
  if (stage.nose === 'dome') {
    appendDomeNose(g, stage);
  } else {
    appendConeNose(g, stage);
  }
  if (stage.nose === 'capsule') {
    appendEscapeTower(g, stage);
  }
  return g;
}

/**
 * Two strap-on boosters, one on each side of the hull.
 */
function appendBoosters(g: SVGGElement, stage: ShipStage): void {
  const w = stage.w;
  for (const dir of [-1, 1]) {
    appendBooster(g, dir * (w + w * 0.42), w * 0.42, stage.boost);
  }
}

/**
 * Fins in ink, their outer edge in amber.
 */
function appendFins(g: SVGGElement, stage: ShipStage): void {
  const fh = finHeight(stage);
  for (const dir of [-1, 1]) {
    const root = dir * stage.w;
    const tip = dir * stage.w * 1.9;
    g.append(
      svgElement('path', {
        d: `M${root} ${SKIRT} L${tip} ${SKIRT - 2} L${root} ${SKIRT + fh} Z`,
        fill: ROCKET_PALETTE.ink,
      }),
      svgElement('path', {
        d: `M${tip} ${SKIRT - 2} L${root} ${SKIRT + fh}`,
        stroke: ROCKET_PALETTE.brand,
        'stroke-width': 1.6,
      }),
    );
  }
}

/**
 * Skirt under the hull and its engine bells: one large bell, or three smaller ones.
 */
function appendSkirt(g: SVGGElement, stage: ShipStage): void {
  const w = stage.w;
  g.append(
    svgElement('path', {
      d: `M${-w} ${SKIRT} L${-w * 1.12} 0 L${w * 1.12} 0 L${w} ${SKIRT} Z`,
      fill: ROCKET_PALETTE.steelDark,
      stroke: ROCKET_PALETTE.steelLit,
      'stroke-width': 1,
    }),
  );
  const singleEngine = stage.eng === 1;
  const bellCentres = singleEngine ? [0] : [-w * 0.55, 0, w * 0.55];
  const r = singleEngine ? w * 0.5 : w * 0.3;
  for (const ex of bellCentres) {
    g.append(
      svgElement('path', {
        d: `M${ex - r * 0.6} 11 L${ex - r} 1 L${ex + r} 1 L${ex + r * 0.6} 11 Z`,
        fill: ROCKET_PALETTE.nozzle,
        stroke: ROCKET_PALETTE.steelLit,
        'stroke-width': 0.8,
      }),
    );
  }
}

/**
 * The hull and everything painted on it, then its cylinder shading.
 */
function appendHull(g: SVGGElement, stage: ShipStage, stageIndex: number): void {
  const w = stage.w;
  g.append(
    svgElement('rect', {
      x: -w,
      y: SKIRT,
      width: w * 2,
      height: stage.h,
      fill: ROCKET_PALETTE.hull,
    }),
  );
  if (stage.fins) {
    appendRollPattern(g, stage);
  }
  appendSeams(g, stage, stageIndex);
  if (stage.bands) {
    appendLivery(g, stage);
  }
  if (stage.ports) {
    appendPortholes(g, stage);
  }
  appendShade(g, -w, w, SKIRT, hullTop(stage));
}

/**
 * Roll pattern at the foot: black and white quarters, the mark of the great launchers.
 */
function appendRollPattern(g: SVGGElement, stage: ShipStage): void {
  const w = stage.w;
  const cellH = Math.min(9, stage.h * 0.07);
  for (let row = 0; row < 2; row++) {
    for (let col = 0; col < 4; col++) {
      const isInkCell = (row + col) % 2 === 0;
      if (!isInkCell) {
        continue;
      }
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

/**
 * One ring per guardian defeated, the seams between them visible.
 */
function appendSeams(g: SVGGElement, stage: ShipStage, stageIndex: number): void {
  const w = stage.w;
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
}

/**
 * Livery, from the sixth stage: an ink interstage and the amber band of the base.
 */
function appendLivery(g: SVGGElement, stage: ShipStage): void {
  const w = stage.w;
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

/**
 * Portholes: life aboard, amber like everywhere else, each glowing at its own pace.
 */
function appendPortholes(g: SVGGElement, stage: ShipStage): void {
  const w = stage.w;
  const rows = stage.ports * 2;
  for (let r = 0; r < rows; r++) {
    const port = svgElement('circle', {
      cx: -w * 0.2,
      cy: SKIRT + stage.h * (0.3 + (r / rows) * 0.6),
      r: Math.max(1.4, w * 0.11),
      fill: ROCKET_PALETTE.warm,
      stroke: ROCKET_PALETTE.ink,
      'stroke-width': 0.8,
    });
    port.append(animate('0.95;0.6;0.95', `${(3 + r * 0.4).toFixed(1)}s`));
    g.append(port);
  }
}

/**
 * Rounded nose of the first stages, all in amber, its right half shaded.
 */
function appendDomeNose(g: SVGGElement, stage: ShipStage): void {
  const w = stage.w;
  const top = hullTop(stage);
  const tip = noseTip(stage);
  g.append(
    svgElement('path', {
      d: `M${-w} ${top} Q${-w} ${tip} 0 ${tip} Q${w} ${tip} ${w} ${top} Z`,
      fill: ROCKET_PALETTE.brand,
    }),
    svgElement('path', {
      d: `M0 ${top} L0 ${tip} Q${w} ${tip} ${w} ${top} Z`,
      fill: ROCKET_PALETTE.shade,
      opacity: 0.28,
    }),
  );
}

/**
 * Pointed nose in ivory with an amber tip, its right half shaded like the hull.
 */
function appendConeNose(g: SVGGElement, stage: ShipStage): void {
  const w = stage.w;
  const top = hullTop(stage);
  const tip = noseTip(stage);
  const amberFrom = top + noseHeight(stage) * 0.62;
  g.append(
    svgElement('path', {
      d: `M${-w} ${top} L0 ${tip} L${w} ${top} Z`,
      fill: ROCKET_PALETTE.hull,
    }),
    svgElement('path', {
      d: `M${-w * 0.38} ${amberFrom} L0 ${tip} L${w * 0.38} ${amberFrom} Z`,
      fill: ROCKET_PALETTE.brand,
    }),
    svgElement('path', {
      d: `M0 ${top} L0 ${tip} L${w} ${top} Z`,
      fill: ROCKET_PALETTE.shade,
      opacity: 0.3,
    }),
  );
}

/**
 * Escape tower, the last part fitted: a lattice mast and its motor on top of the nose.
 */
function appendEscapeTower(g: SVGGElement, stage: ShipStage): void {
  const w = stage.w;
  const base = noseTip(stage);
  const mast = w * 1.6;
  const motorTop = base + mast + w * 0.3;
  g.append(
    svgElement('path', {
      d: towerLattice(base, mast),
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
      d: `M-2 ${motorTop} L0 ${base + w * 2.2} L2 ${motorTop} Z`,
      fill: ROCKET_PALETTE.brand,
    }),
  );
}

/**
 * Two converging rails joined by four diagonal braces.
 */
function towerLattice(base: number, mast: number): string {
  const rails = `M-3 ${base} L-1.2 ${base + mast} M3 ${base} L1.2 ${base + mast}`;
  let braces = '';
  for (let k = 0; k < 4; k++) {
    const y0 = base + (mast / 4) * k;
    const y1 = y0 + mast / 4;
    braces += ` M${-3 + 0.45 * k} ${y0} L${2.55 - 0.45 * k} ${y1}`;
  }
  return rails + braces;
}

/**
 * Flat cylinder shading: a lit strip on the left, two shade steps on the right.
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
 * Service gantry on one side only: masts on both sides would cage the rocket.
 */
function appendGantry(g: SVGGElement, stage: ShipStage): void {
  const complete = stage.gantry === 2;
  const gx = -(shipHalf(stage) + GANTRY_GAP);
  const gh = stage.h * (complete ? 0.86 : 0.7);
  const left = gx - 5;
  const right = gx + 11;

  appendGantryTower(g, left, right, gh);
  appendGantryArms(g, stage, right, gh);
  if (complete) {
    appendGantryJib(g, stage, gx, left, gh);
  }
  const beacon = svgElement('circle', {
    cx: gx + 3,
    cy: gh + (complete ? 14 : 4),
    r: 2.6,
    fill: ROCKET_PALETTE.red,
  });
  beacon.append(animate('1;0.15;1', '2.6s'));
  g.append(beacon);
}

/**
 * The gantry's lattice tower: two masts from `left` to `right`, cross-braced up to `height`.
 */
function appendGantryTower(g: SVGGElement, left: number, right: number, height: number): void {
  const step = GANTRY_BRACE_STEP;
  let lattice = `M${left} 0 V${height} M${right} 0 V${height}`;
  for (let y = 0; y + step <= height; y += step) {
    lattice += ` M${left} ${y} H${right} M${left} ${y} L${right} ${y + step} M${right} ${y} L${left} ${y + step}`;
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
    svgElement('rect', { x: left - 1, y: 0, width: 2.5, height, fill: ROCKET_PALETTE.mast }),
    svgElement('rect', { x: right - 1.5, y: 0, width: 2.5, height, fill: ROCKET_PALETTE.mast }),
  );
}

/**
 * Service arms from the tower to the hull, only those under the tower's top.
 */
function appendGantryArms(g: SVGGElement, stage: ShipStage, right: number, height: number): void {
  for (const ay of GANTRY_ARM_HEIGHTS[stage.gantry]) {
    if (ay > height) {
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
}

/**
 * Hammerhead jib of a complete gantry, its counterweight on the far side.
 */
function appendGantryJib(
  g: SVGGElement,
  stage: ShipStage,
  gx: number,
  left: number,
  height: number,
): void {
  g.append(
    svgElement('rect', {
      x: left - 16,
      y: height + 4,
      width: -left + 16 - stage.w * 0.4,
      height: 3,
      fill: ROCKET_PALETTE.mast,
    }),
    svgElement('rect', {
      x: left - 16,
      y: height - 4,
      width: 9,
      height: 8,
      fill: ROCKET_PALETTE.mast,
    }),
    svgElement('rect', { x: gx + 1, y: height, width: 3, height: 12, fill: ROCKET_PALETTE.mast }),
  );
}

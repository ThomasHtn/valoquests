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
function shipHalfWidth(stage: ShipStage): number {
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
  const halfWidth = stage.w;
  const body = `M${-halfWidth} ${SKIRT} L${-halfWidth * 1.1} 0 L${halfWidth * 1.1} 0 L${halfWidth} ${SKIRT} L${halfWidth} ${top}`;
  const nose =
    stage.nose === 'dome'
      ? ` Q${halfWidth} ${tip} 0 ${tip} Q${-halfWidth} ${tip} ${-halfWidth} ${top}`
      : ` L0 ${tip} L${-halfWidth} ${top}`;
  return `${body}${nose} Z`;
}

/**
 * One fin, on the left (`side = -1`) or right (`side = 1`) side.
 */
function finOutline(stage: ShipStage, side: number): string {
  const root = side * stage.w;
  const tip = side * stage.w * 1.9;
  return `M${root} ${SKIRT} L${tip} ${SKIRT - 2} L${root} ${SKIRT + finHeight(stage)} Z`;
}

/**
 * One booster with its pointed cap, on the left (`side = -1`) or right (`side = 1`) side.
 */
function boosterOutline(stage: ShipStage, side: number): string {
  const boosterHalfWidth = stage.w * 0.42;
  const centerX = side * (stage.w + boosterHalfWidth);
  const capBase = 4 + stage.boost;
  return (
    `M${centerX - boosterHalfWidth} 4 L${centerX - boosterHalfWidth} ${capBase} L${centerX} ${capBase + boosterHalfWidth * 2}` +
    ` L${centerX + boosterHalfWidth} ${capBase} L${centerX + boosterHalfWidth} 4 Z`
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
 * Drawn upward from `y = 0`: the caller places it under a `scale(1 -1)` transform.
 */
export function drawShip(stageIndex: number): SVGGElement {
  const stage = SHIP[stageIndex];
  const group = svgElement('g');
  if (!stageIndex) {
    return group;
  }

  // Back to front: what stands behind the hull is drawn first.
  if (stage.gantry) {
    appendGantry(group, stage);
  }
  if (stage.boost) {
    appendBoosters(group, stage);
  }
  if (stage.fins) {
    appendFins(group, stage);
  }
  appendSkirt(group, stage);
  appendHull(group, stage, stageIndex);
  if (stage.nose === 'dome') {
    appendDomeNose(group, stage);
  } else {
    appendConeNose(group, stage);
  }
  if (stage.nose === 'capsule') {
    appendEscapeTower(group, stage);
  }
  return group;
}

/**
 * Two strap-on boosters, one on each side of the hull.
 */
function appendBoosters(group: SVGGElement, stage: ShipStage): void {
  const halfWidth = stage.w;
  for (const side of [-1, 1]) {
    appendBooster(group, side * (halfWidth + halfWidth * 0.42), halfWidth * 0.42, stage.boost);
  }
}

/**
 * Fins in ink, their outer edge in amber.
 */
function appendFins(group: SVGGElement, stage: ShipStage): void {
  const height = finHeight(stage);
  for (const side of [-1, 1]) {
    const root = side * stage.w;
    const tip = side * stage.w * 1.9;
    group.append(
      svgElement('path', {
        d: `M${root} ${SKIRT} L${tip} ${SKIRT - 2} L${root} ${SKIRT + height} Z`,
        fill: ROCKET_PALETTE.ink,
      }),
      svgElement('path', {
        d: `M${tip} ${SKIRT - 2} L${root} ${SKIRT + height}`,
        stroke: ROCKET_PALETTE.brand,
        'stroke-width': 1.6,
      }),
    );
  }
}

/**
 * Skirt under the hull and its engine bells: one large bell, or three smaller ones.
 */
function appendSkirt(group: SVGGElement, stage: ShipStage): void {
  const halfWidth = stage.w;
  group.append(
    svgElement('path', {
      d: `M${-halfWidth} ${SKIRT} L${-halfWidth * 1.12} 0 L${halfWidth * 1.12} 0 L${halfWidth} ${SKIRT} Z`,
      fill: ROCKET_PALETTE.steelDark,
      stroke: ROCKET_PALETTE.steelLit,
      'stroke-width': 1,
    }),
  );
  const singleEngine = stage.eng === 1;
  const bellCentres = singleEngine ? [0] : [-halfWidth * 0.55, 0, halfWidth * 0.55];
  const bellRadius = singleEngine ? halfWidth * 0.5 : halfWidth * 0.3;
  for (const bellX of bellCentres) {
    group.append(
      svgElement('path', {
        d: `M${bellX - bellRadius * 0.6} 11 L${bellX - bellRadius} 1 L${bellX + bellRadius} 1 L${bellX + bellRadius * 0.6} 11 Z`,
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
function appendHull(group: SVGGElement, stage: ShipStage, stageIndex: number): void {
  const halfWidth = stage.w;
  group.append(
    svgElement('rect', {
      x: -halfWidth,
      y: SKIRT,
      width: halfWidth * 2,
      height: stage.h,
      fill: ROCKET_PALETTE.hull,
    }),
  );
  if (stage.fins) {
    appendRollPattern(group, stage);
  }
  appendSeams(group, stage, stageIndex);
  if (stage.bands) {
    appendLivery(group, stage);
  }
  if (stage.ports) {
    appendPortholes(group, stage);
  }
  appendShade(group, -halfWidth, halfWidth, SKIRT, hullTop(stage));
}

/**
 * Roll pattern at the foot: black and white quarters, the mark of the great launchers.
 */
function appendRollPattern(group: SVGGElement, stage: ShipStage): void {
  const halfWidth = stage.w;
  const cellH = Math.min(9, stage.h * 0.07);
  for (let row = 0; row < 2; row++) {
    for (let col = 0; col < 4; col++) {
      const isInkCell = (row + col) % 2 === 0;
      if (!isInkCell) {
        continue;
      }
      group.append(
        svgElement('rect', {
          x: -halfWidth + (col * halfWidth) / 2,
          y: SKIRT + 3 + row * cellH,
          width: halfWidth / 2,
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
function appendSeams(group: SVGGElement, stage: ShipStage, stageIndex: number): void {
  const halfWidth = stage.w;
  for (let ring = 1; ring < stageIndex; ring++) {
    const y = SKIRT + (stage.h / stageIndex) * ring;
    group.append(
      svgElement('rect', {
        x: -halfWidth,
        y: y - 0.7,
        width: halfWidth * 2,
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
function appendLivery(group: SVGGElement, stage: ShipStage): void {
  const halfWidth = stage.w;
  group.append(
    svgElement('rect', {
      x: -halfWidth,
      y: SKIRT + stage.h * 0.34,
      width: halfWidth * 2,
      height: 6,
      fill: ROCKET_PALETTE.ink,
    }),
    svgElement('rect', {
      x: -halfWidth,
      y: SKIRT + stage.h * 0.62,
      width: halfWidth * 2,
      height: 8,
      fill: ROCKET_PALETTE.brand,
    }),
  );
}

/**
 * Portholes: life aboard, amber like everywhere else, each glowing at its own pace.
 */
function appendPortholes(group: SVGGElement, stage: ShipStage): void {
  const halfWidth = stage.w;
  const rows = stage.ports * 2;
  for (let index = 0; index < rows; index++) {
    const port = svgElement('circle', {
      cx: -halfWidth * 0.2,
      cy: SKIRT + stage.h * (0.3 + (index / rows) * 0.6),
      r: Math.max(1.4, halfWidth * 0.11),
      fill: ROCKET_PALETTE.warm,
      stroke: ROCKET_PALETTE.ink,
      'stroke-width': 0.8,
    });
    port.append(animate('0.95;0.6;0.95', `${(3 + index * 0.4).toFixed(1)}s`));
    group.append(port);
  }
}

/**
 * Rounded nose of the first stages, all in amber, its right half shaded.
 */
function appendDomeNose(group: SVGGElement, stage: ShipStage): void {
  const halfWidth = stage.w;
  const top = hullTop(stage);
  const tip = noseTip(stage);
  group.append(
    svgElement('path', {
      d: `M${-halfWidth} ${top} Q${-halfWidth} ${tip} 0 ${tip} Q${halfWidth} ${tip} ${halfWidth} ${top} Z`,
      fill: ROCKET_PALETTE.brand,
    }),
    svgElement('path', {
      d: `M0 ${top} L0 ${tip} Q${halfWidth} ${tip} ${halfWidth} ${top} Z`,
      fill: ROCKET_PALETTE.shade,
      opacity: 0.28,
    }),
  );
}

/**
 * Pointed nose in ivory with an amber tip, its right half shaded like the hull.
 */
function appendConeNose(group: SVGGElement, stage: ShipStage): void {
  const halfWidth = stage.w;
  const top = hullTop(stage);
  const tip = noseTip(stage);
  const amberFrom = top + noseHeight(stage) * 0.62;
  group.append(
    svgElement('path', {
      d: `M${-halfWidth} ${top} L0 ${tip} L${halfWidth} ${top} Z`,
      fill: ROCKET_PALETTE.hull,
    }),
    svgElement('path', {
      d: `M${-halfWidth * 0.38} ${amberFrom} L0 ${tip} L${halfWidth * 0.38} ${amberFrom} Z`,
      fill: ROCKET_PALETTE.brand,
    }),
    svgElement('path', {
      d: `M0 ${top} L0 ${tip} L${halfWidth} ${top} Z`,
      fill: ROCKET_PALETTE.shade,
      opacity: 0.3,
    }),
  );
}

/**
 * Escape tower, the last part fitted: a lattice mast and its motor on top of the nose.
 */
function appendEscapeTower(group: SVGGElement, stage: ShipStage): void {
  const halfWidth = stage.w;
  const base = noseTip(stage);
  const mast = halfWidth * 1.6;
  const motorTop = base + mast + halfWidth * 0.3;
  group.append(
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
      height: halfWidth * 0.3,
      fill: ROCKET_PALETTE.ink,
    }),
    svgElement('path', {
      d: `M-2 ${motorTop} L0 ${base + halfWidth * 2.2} L2 ${motorTop} Z`,
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
  for (let brace = 0; brace < 4; brace++) {
    const braceBottom = base + (mast / 4) * brace;
    const braceTop = braceBottom + mast / 4;
    braces += ` M${-3 + 0.45 * brace} ${braceBottom} L${2.55 - 0.45 * brace} ${braceTop}`;
  }
  return rails + braces;
}

/**
 * Flat cylinder shading: a lit strip on the left, two shade steps on the right.
 */
function appendShade(group: SVGGElement, x0: number, x1: number, y0: number, y1: number): void {
  const width = x1 - x0;
  const height = y1 - y0;
  group.append(
    svgElement('rect', {
      x: x0 + width * 0.14,
      y: y0,
      width: width * 0.14,
      height,
      fill: ROCKET_PALETTE.hullLit,
      opacity: 0.35,
    }),
    svgElement('rect', {
      x: x0 + width * 0.6,
      y: y0,
      width: width * 0.4,
      height,
      fill: ROCKET_PALETTE.shade,
      opacity: 0.2,
    }),
    svgElement('rect', {
      x: x0 + width * 0.84,
      y: y0,
      width: width * 0.16,
      height,
      fill: ROCKET_PALETTE.shade,
      opacity: 0.22,
    }),
  );
}

/**
 * A strap-on booster, behind the hull: ivory body, ink foot, amber cap.
 */
function appendBooster(
  group: SVGGElement,
  centerX: number,
  halfWidth: number,
  height: number,
): void {
  group.append(
    svgElement('rect', {
      x: centerX - halfWidth,
      y: 4,
      width: halfWidth * 2,
      height,
      fill: ROCKET_PALETTE.hull,
    }),
    svgElement('rect', {
      x: centerX - halfWidth,
      y: 4,
      width: halfWidth * 2,
      height: Math.min(10, height * 0.1),
      fill: ROCKET_PALETTE.ink,
    }),
    svgElement('path', {
      d: `M${centerX - halfWidth} ${4 + height} L${centerX} ${4 + height + halfWidth * 2.1} L${centerX + halfWidth} ${4 + height} Z`,
      fill: ROCKET_PALETTE.brand,
    }),
    svgElement('rect', {
      x: centerX - halfWidth,
      y: 0,
      width: halfWidth * 2,
      height: 4,
      fill: ROCKET_PALETTE.nozzle,
    }),
  );
  appendShade(group, centerX - halfWidth, centerX + halfWidth, 4, 4 + height);
}

/**
 * Service gantry on one side only: masts on both sides would cage the rocket.
 */
function appendGantry(group: SVGGElement, stage: ShipStage): void {
  const complete = stage.gantry === 2;
  const towerX = -(shipHalfWidth(stage) + GANTRY_GAP);
  const towerHeight = stage.h * (complete ? 0.86 : 0.7);
  const left = towerX - 5;
  const right = towerX + 11;

  appendGantryTower(group, left, right, towerHeight);
  appendGantryArms(group, stage, right, towerHeight);
  if (complete) {
    appendGantryJib(group, stage, towerX, left, towerHeight);
  }
  const beacon = svgElement('circle', {
    cx: towerX + 3,
    cy: towerHeight + (complete ? 14 : 4),
    r: 2.6,
    fill: ROCKET_PALETTE.red,
  });
  beacon.append(animate('1;0.15;1', '2.6s'));
  group.append(beacon);
}

/**
 * The gantry's lattice tower: two masts from `left` to `right`, cross-braced up to `height`.
 */
function appendGantryTower(group: SVGGElement, left: number, right: number, height: number): void {
  const step = GANTRY_BRACE_STEP;
  let lattice = `M${left} 0 V${height} M${right} 0 V${height}`;
  for (let y = 0; y + step <= height; y += step) {
    lattice += ` M${left} ${y} H${right} M${left} ${y} L${right} ${y + step} M${right} ${y} L${left} ${y + step}`;
  }
  group.append(
    svgElement('path', {
      d: lattice,
      stroke: ROCKET_PALETTE.mast,
      'stroke-width': 1.3,
      fill: 'none',
    }),
  );
  group.append(
    svgElement('rect', { x: left - 1, y: 0, width: 2.5, height, fill: ROCKET_PALETTE.mast }),
    svgElement('rect', { x: right - 1.5, y: 0, width: 2.5, height, fill: ROCKET_PALETTE.mast }),
  );
}

/**
 * Service arms from the tower to the hull, only those under the tower's top.
 */
function appendGantryArms(
  group: SVGGElement,
  stage: ShipStage,
  right: number,
  height: number,
): void {
  for (const armY of GANTRY_ARM_HEIGHTS[stage.gantry]) {
    if (armY > height) {
      continue;
    }
    group.append(
      svgElement('rect', {
        x: right,
        y: armY,
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
  group: SVGGElement,
  stage: ShipStage,
  towerX: number,
  left: number,
  height: number,
): void {
  group.append(
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
    svgElement('rect', {
      x: towerX + 1,
      y: height,
      width: 3,
      height: 12,
      fill: ROCKET_PALETTE.mast,
    }),
  );
}

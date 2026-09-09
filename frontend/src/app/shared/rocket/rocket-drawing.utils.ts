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
  const bw = w * 0.42;

  // Service gantry on one side only: two masts either side make a cage the rocket vanishes into.
  if (stage.gantry) {
    const gx = -(shipHalf(stage) + 22);
    const gh = stage.h * (stage.gantry === 2 ? 0.86 : 0.7);
    g.append(
      svgElement('rect', { x: gx - 5, y: 0, width: 4, height: gh, fill: ROCKET_PALETTE.mast }),
    );
    g.append(
      svgElement('rect', { x: gx + 9, y: 0, width: 4, height: gh, fill: ROCKET_PALETTE.mast }),
    );
    for (let y = 10; y < gh; y += 18) {
      g.append(
        svgElement('rect', {
          x: gx - 5,
          y,
          width: 18,
          height: 1.4,
          fill: ROCKET_PALETTE.mast,
          opacity: 0.7,
        }),
      );
    }
    const arms = stage.gantry === 2 ? [26, 74, 122, 170] : [26, 78];
    for (const ay of arms) {
      if (ay > gh) {
        continue;
      }
      g.append(
        svgElement('rect', {
          x: gx + 13,
          y: ay,
          width: -gx - shipHalf(stage) - 5,
          height: 2.6,
          fill: ROCKET_PALETTE.mast,
        }),
      );
    }
    const beacon = svgElement('circle', {
      cx: gx + 4,
      cy: gh + 4,
      r: 2.6,
      fill: ROCKET_PALETTE.red,
    });
    beacon.append(animate('1;0.15;1', '2.6s'));
    g.append(beacon);
  }

  // Strap-on boosters, behind the hull.
  if (stage.boost) {
    for (const dir of [-1, 1]) {
      const cx = dir * (w + bw);
      g.append(
        svgElement('rect', {
          x: cx - bw,
          y: 4,
          width: bw * 2,
          height: stage.boost,
          fill: ROCKET_PALETTE.steelDark,
          stroke: ROCKET_PALETTE.steelLit,
          'stroke-width': 1,
        }),
      );
      g.append(
        svgElement('path', {
          d: `M${cx - bw} ${4 + stage.boost} L${cx} ${4 + stage.boost + bw * 2.1} L${cx + bw} ${4 + stage.boost} Z`,
          fill: ROCKET_PALETTE.steel,
          stroke: ROCKET_PALETTE.cyan,
          'stroke-width': 1,
          'stroke-opacity': 0.5,
        }),
      );
      g.append(
        svgElement('rect', {
          x: cx - bw,
          y: 2,
          width: bw * 2,
          height: 5,
          fill: ROCKET_PALETTE.steelLit,
        }),
      );
    }
  }

  // Fins.
  if (stage.fins) {
    const fh = Math.max(14, stage.h * 0.26);
    for (const dir of [-1, 1]) {
      g.append(
        svgElement('path', {
          d: `M${dir * w} ${SKIRT} L${dir * w * 1.9} ${SKIRT - 2} L${dir * w} ${SKIRT + fh} Z`,
          fill: ROCKET_PALETTE.steel,
          stroke: ROCKET_PALETTE.cyan,
          'stroke-width': 1,
          'stroke-opacity': 0.45,
        }),
      );
    }
  }

  // Skirt and engines.
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
        fill: '#0c141c',
        stroke: ROCKET_PALETTE.steelLit,
        'stroke-width': 0.8,
      }),
    );
  }

  // The hull in rings, one per guardian defeated, the seam between them visible.
  g.append(
    svgElement('rect', {
      x: -w,
      y: SKIRT,
      width: w * 2,
      height: stage.h,
      fill: ROCKET_PALETTE.steel,
      stroke: ROCKET_PALETTE.cyan,
      'stroke-width': 1.5,
    }),
  );
  g.append(
    svgElement('rect', {
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
    g.append(
      svgElement('rect', {
        x: -w,
        y: y - 1.5,
        width: w * 2,
        height: 3,
        fill: ROCKET_PALETTE.steelLit,
      }),
    );
  }

  // Marking bands, from the sixth stage.
  if (stage.bands) {
    g.append(
      svgElement('rect', {
        x: -w,
        y: SKIRT + stage.h * 0.62,
        width: w * 2,
        height: 6,
        fill: ROCKET_PALETTE.brand,
        opacity: 0.85,
      }),
    );
    g.append(
      svgElement('rect', {
        x: -w,
        y: SKIRT + stage.h * 0.2,
        width: w * 2,
        height: 3,
        fill: ROCKET_PALETTE.warmCore,
        opacity: 0.55,
      }),
    );
  }

  // Portholes: life aboard, amber like everywhere else.
  if (stage.ports) {
    const rows = stage.ports * 2;
    for (let r = 0; r < rows; r++) {
      const y = SKIRT + stage.h * (0.3 + (r / rows) * 0.6);
      const port = svgElement('circle', {
        cx: 0,
        cy: y,
        r: Math.max(1.6, w * 0.16),
        fill: ROCKET_PALETTE.warm,
      });
      port.append(animate('0.95;0.55;0.95', `${(3 + r * 0.4).toFixed(1)}s`));
      g.append(port);
    }
  }

  // The nose.
  if (stage.nose === 'dome') {
    g.append(
      svgElement('path', {
        d: `M${-w} ${top} Q${-w} ${top + nh} 0 ${top + nh} Q${w} ${top + nh} ${w} ${top} Z`,
        fill: ROCKET_PALETTE.steelLit,
      }),
    );
  } else {
    g.append(
      svgElement('path', {
        d: `M${-w} ${top} L0 ${top + nh} L${w} ${top} Z`,
        fill: ROCKET_PALETTE.steelLit,
        stroke: ROCKET_PALETTE.cyan,
        'stroke-width': 1.2,
      }),
    );
  }

  // Capsule and escape tower, the last two parts fitted.
  if (stage.nose === 'capsule') {
    const capBase = top + nh;
    g.append(
      svgElement('rect', {
        x: -2,
        y: capBase,
        width: 4,
        height: w * 1.6,
        fill: ROCKET_PALETTE.mast,
      }),
    );
    g.append(
      svgElement('path', {
        d: `M-5 ${capBase + w * 1.6} L0 ${capBase + w * 2.2} L5 ${capBase + w * 1.6} Z`,
        fill: ROCKET_PALETTE.brand,
      }),
    );
  }

  return g;
}

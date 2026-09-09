import { animate, drawShip, outline } from '@shared/rocket/rocket-drawing.utils';
import { SHIP } from '@shared/rocket/rocket-drawing.constants';
import { createSeededRandom } from '@core/random/seeded-random.utils';
import { TownSceneInputs, Facade } from './town-scene.model';
import { svgElement } from '@core/svg/svg-element.utils';
import {
  TOWN_PALETTE,
  TOWN_SEED,
  TOWN_WIDTH,
  TOWN_HEIGHT,
  HORIZON,
  RX,
  SHIP_SCALE,
  PLOT,
  SKYLINE,
  SPREAD,
  RISE,
  SEEDLING,
  CLARITY,
} from './town-scene.constants';

/**
 * Builds the night-time base and its rocket as SVG nodes.
 *
 * The city is the score, so the city has to grow: one number, the population read against what
 * a full campaign produces, drives how many buildings stand, how tall they are, their style and
 * how many windows are lit. Two settings that could disagree would end up disagreeing on screen.
 *
 * Pure DOM construction with no Angular dependency, kept apart from the component so the drawing
 * can be read as a drawing.
 */

/**
 * Draws the whole scene into an empty `<svg>`.
 *
 * @param svg - The element to fill; anything it holds is replaced.
 * @param inputs - What the scene is drawn from.
 */
export function buildTownScene(svg: SVGSVGElement, inputs: TownSceneInputs): void {
  const rnd = createSeededRandom(TOWN_SEED);
  const growth = Math.min(
    1,
    Math.max(0.08, inputs.population / Math.max(1, inputs.fullCampaignPopulation)),
  );
  const litShare = 0.45 + growth * 0.35;
  const stagesDone = Math.max(0, Math.min(SHIP.length - 1, inputs.stagesDone));

  const frag = document.createDocumentFragment();
  const add = <T extends Node>(node: T): T => {
    frag.appendChild(node);
    return node;
  };

  const defs = add(svgElement('defs'));
  const sky = svgElement('linearGradient', { id: 'town-sky', x1: 0, y1: 0, x2: 0, y2: 1 });
  sky.append(
    svgElement('stop', { offset: 0, 'stop-color': TOWN_PALETTE.night }),
    svgElement('stop', { offset: 0.6, 'stop-color': '#08131c' }),
    svgElement('stop', { offset: 1, 'stop-color': '#132430' }),
  );
  const glow = svgElement('radialGradient', { id: 'town-glow', cx: 0.5, cy: 0.5, r: 0.5 });
  glow.append(
    svgElement('stop', { offset: 0, 'stop-color': TOWN_PALETTE.brand, 'stop-opacity': 0.5 }),
    svgElement('stop', { offset: 1, 'stop-color': TOWN_PALETTE.brand, 'stop-opacity': 0 }),
  );
  const pad = svgElement('radialGradient', { id: 'town-pad', cx: 0.5, cy: 1, r: 0.7 });
  pad.append(
    svgElement('stop', { offset: 0, 'stop-color': TOWN_PALETTE.warm, 'stop-opacity': 0.22 }),
    svgElement('stop', { offset: 1, 'stop-color': TOWN_PALETTE.warm, 'stop-opacity': 0 }),
  );
  const sea = svgElement('linearGradient', { id: 'town-sea', x1: 0, y1: 0, x2: 0, y2: 1 });
  sea.append(
    svgElement('stop', { offset: 0, 'stop-color': '#123340' }),
    svgElement('stop', { offset: 0.35, 'stop-color': '#081820' }),
    svgElement('stop', { offset: 1, 'stop-color': '#050f15' }),
  );
  const vignette = svgElement('linearGradient', {
    id: 'town-vignette',
    x1: 0,
    y1: 0,
    x2: 1,
    y2: 0,
  });
  vignette.append(
    svgElement('stop', { offset: 0, 'stop-color': TOWN_PALETTE.night, 'stop-opacity': 0.85 }),
    svgElement('stop', { offset: 0.26, 'stop-color': TOWN_PALETTE.night, 'stop-opacity': 0 }),
    svgElement('stop', { offset: 0.76, 'stop-color': TOWN_PALETTE.night, 'stop-opacity': 0 }),
    svgElement('stop', { offset: 1, 'stop-color': TOWN_PALETTE.night, 'stop-opacity': 0.85 }),
  );
  defs.append(sky, glow, pad, sea, vignette);

  add(
    svgElement('rect', {
      x: 0,
      y: 0,
      width: TOWN_WIDTH,
      height: TOWN_HEIGHT,
      fill: 'url(#town-sky)',
    }),
  );

  // Stars: a fixed sequence cut by the clarity. Faded stars read as a rendering bug, missing ones
  // as a bad night.
  const starCount = Math.round(16 + CLARITY * 62);
  for (let i = 0; i < starCount; i++) {
    add(
      svgElement('circle', {
        cx: (rnd() * TOWN_WIDTH).toFixed(1),
        cy: (rnd() * 210).toFixed(1),
        r: rnd() < 0.12 ? 1.5 : 0.9,
        fill: '#cfe4ee',
        opacity: (0.35 + rnd() * 0.5).toFixed(2),
      }),
    );
  }

  add(
    svgElement('ellipse', {
      cx: RX,
      cy: HORIZON,
      rx: 560,
      ry: 130,
      fill: 'url(#town-glow)',
      opacity: 0.5,
    }),
  );

  // Distant skyline, kept off the plot: the rocket stands against the sky, never against roofs.
  let x = -20;
  const farPath: string[] = [];
  while (x < TOWN_WIDTH + 40) {
    const w = 26 + rnd() * 52;
    const h = 20 + rnd() * 52;
    if (x + w < PLOT[0] || x > PLOT[1]) {
      farPath.push(
        `M${x.toFixed(0)} ${HORIZON} V${(HORIZON - h).toFixed(0)} H${(x + w).toFixed(0)} V${HORIZON}`,
      );
    }
    x += w + rnd() * 14;
  }
  add(svgElement('path', { d: farPath.join(' '), fill: TOWN_PALETTE.far }));
  add(
    svgElement('rect', {
      x: 0,
      y: HORIZON - 70,
      width: TOWN_WIDTH,
      height: 70,
      fill: TOWN_PALETTE.hazeFar,
      opacity: 0.45,
    }),
  );

  // Buildings, and the windows that carry the population.
  const windows: Facade[] = [];

  const block = (bx: number, bw: number, bh: number, dark: boolean, sign: boolean): SVGGElement => {
    const top = HORIZON - bh;
    const g = svgElement('g');
    g.append(
      svgElement('rect', {
        x: bx,
        y: top,
        width: bw,
        height: bh,
        fill: dark ? TOWN_PALETTE.wall : TOWN_PALETTE.wallLit,
      }),
    );
    g.append(svgElement('rect', { x: bx, y: top, width: bw, height: 4, fill: TOWN_PALETTE.roof }));
    g.append(
      svgElement('rect', {
        x: bx + bw - 4,
        y: top,
        width: 4,
        height: bh,
        fill: '#0d141b',
        opacity: 0.7,
      }),
    );

    // Three styles told apart by height alone: a house takes a pitched roof, a block stays flat, a
    // tower gains a light edge on its facade. The city changes character as it grows, not only
    // size.
    if (bh < 46) {
      g.append(
        svgElement('path', {
          d:
            `M${(bx - 3).toFixed(1)} ${(top + 1).toFixed(1)} ` +
            `L${(bx + bw / 2).toFixed(1)} ${(top - 10).toFixed(1)} ` +
            `L${(bx + bw + 3).toFixed(1)} ${(top + 1).toFixed(1)} Z`,
          fill: TOWN_PALETTE.roof,
        }),
      );
    } else if (bh >= 110) {
      g.append(
        svgElement('rect', {
          x: bx + 3,
          y: top + 5,
          width: 1.5,
          height: bh - 5,
          fill: TOWN_PALETTE.steelLit,
          opacity: 0.55,
        }),
      );
    }

    // Windows as horizontal strips, never squares: two squares above a door make a face, and that
    // is what makes a drawn city childish.
    const floorH = 15;
    const rows = Math.max(1, Math.floor((bh - 18) / floorH));
    const cols = Math.max(1, Math.floor((bw - 12) / 14));
    for (let r = 0; r < rows; r++) {
      const wy = top + 11 + r * floorH;
      if (wy > HORIZON - 10) {
        continue;
      }
      const floorRoll = rnd();
      for (let c = 0; c < cols; c++) {
        windows.push({
          x: bx + 7 + c * 14,
          y: wy,
          w: 10,
          h: 4,
          roll: floorRoll * 0.72 + rnd() * 0.28,
        });
      }
    }

    if (bh > 150) {
      g.append(
        svgElement('rect', {
          x: bx + bw / 2 - 1,
          y: top - 24,
          width: 2,
          height: 24,
          fill: TOWN_PALETTE.mast,
        }),
      );
      const beacon = svgElement('circle', {
        cx: bx + bw / 2,
        cy: top - 26,
        r: 2,
        fill: TOWN_PALETTE.red,
        opacity: 0.9,
      });
      beacon.append(animate('0.9;0.1;0.9', `${(2 + rnd() * 2).toFixed(1)}s`));
      g.append(beacon);
    }

    if (sign) {
      g.append(
        svgElement('rect', {
          x: bx + 6,
          y: top + bh * 0.42,
          width: bw - 16,
          height: 3,
          fill: TOWN_PALETTE.cyan,
          opacity: 0.75,
        }),
      );
    }
    return g;
  };

  // The city rises around the pad and spreads toward the edges: the base was founded where the
  // ship is built, and that is where it grows from. A plot appears once the growth reaches its
  // threshold, then rises for a third of a campaign to its final height, so the first houses
  // become towers instead of being replaced.
  for (const [bx, bw, hMax, sign] of SKYLINE) {
    const away = Math.abs(bx + bw / 2 - RX) / RX;
    const born = away * SPREAD;
    if (growth < born) {
      continue;
    }
    const age = Math.min(1, (growth - born) / RISE);
    const bh = Math.max(20, Math.round(hMax * (SEEDLING + (1 - SEEDLING) * age)));
    add(block(bx, bw, bh, rnd() < 0.45, Boolean(sign) && bh > 90));
  }

  // The rocket, in the middle of the village.
  const padGroup = svgElement('g');
  padGroup.append(
    svgElement('ellipse', { cx: RX, cy: HORIZON + 4, rx: 210, ry: 100, fill: 'url(#town-pad)' }),
  );
  padGroup.append(
    svgElement('rect', { x: RX - 150, y: HORIZON - 5, width: 300, height: 12, fill: '#18232e' }),
  );
  padGroup.append(
    svgElement('rect', { x: RX - 150, y: HORIZON - 6, width: 300, height: 1.5, fill: '#2a3947' }),
  );
  for (const lx of [RX - 138, RX + 138]) {
    padGroup.append(
      svgElement('rect', { x: lx, y: HORIZON - 36, width: 2, height: 32, fill: TOWN_PALETTE.mast }),
    );
    padGroup.append(
      svgElement('circle', { cx: lx + 1, cy: HORIZON - 38, r: 3, fill: TOWN_PALETTE.warmCore }),
    );
    padGroup.append(
      svgElement('circle', {
        cx: lx + 1,
        cy: HORIZON - 38,
        r: 14,
        fill: TOWN_PALETTE.warm,
        opacity: 0.14,
      }),
    );
  }
  add(padGroup);

  // Shared frame of both drawings: the pad's ground, the scale, and the vertical axis flipped so the
  // rocket builds upward the way it is read.
  const shipFrame = (): SVGGElement =>
    svgElement('g', {
      transform: `translate(${RX} ${HORIZON - 4}) scale(${SHIP_SCALE} ${-SHIP_SCALE})`,
    });

  // The template of the finished launcher: what remains to be built is there, dotted, at its true
  // size. It is what says at a glance that the rocket is not finished.
  const ghost = shipFrame();
  ghost.append(
    svgElement('path', {
      d: outline(SHIP[SHIP.length - 1]),
      fill: 'none',
      stroke: TOWN_PALETTE.ghost,
      'stroke-width': 1.6,
      'stroke-dasharray': '6 8',
      opacity: 0.45,
    }),
  );
  add(ghost);

  const built = shipFrame();
  built.append(drawShip(stagesDone));
  add(built);

  // Quay, water, reflections.
  add(
    svgElement('rect', {
      x: 0,
      y: HORIZON + 6,
      width: TOWN_WIDTH,
      height: 7,
      fill: TOWN_PALETTE.quayEdge,
    }),
  );
  add(
    svgElement('rect', {
      x: 0,
      y: HORIZON + 13,
      width: TOWN_WIDTH,
      height: 13,
      fill: TOWN_PALETTE.quayFace,
    }),
  );
  add(
    svgElement('rect', {
      x: 0,
      y: HORIZON + 26,
      width: TOWN_WIDTH,
      height: TOWN_HEIGHT - HORIZON - 26,
      fill: 'url(#town-sea)',
    }),
  );

  for (let lx = 44; lx < TOWN_WIDTH; lx += 122) {
    if (lx > PLOT[0] - 40 && lx < PLOT[1] + 40) {
      continue;
    }
    add(
      svgElement('rect', { x: lx, y: HORIZON - 22, width: 2, height: 28, fill: TOWN_PALETTE.mast }),
    );
    add(
      svgElement('circle', { cx: lx + 1, cy: HORIZON - 24, r: 2.6, fill: TOWN_PALETTE.warmCore }),
    );
    add(
      svgElement('circle', {
        cx: lx + 1,
        cy: HORIZON - 24,
        r: 10,
        fill: TOWN_PALETTE.warm,
        opacity: 0.13,
      }),
    );
  }

  // Reflections: broken columns, never continuous streaks.
  const reflect = svgElement('g', { opacity: 0.5 });
  for (let i = 0; i < 40; i++) {
    const rx = rnd() * TOWN_WIDTH;
    const top = HORIZON + 28 + rnd() * 14;
    const warmOne = rnd() < 0.72;
    const segments = 2 + Math.floor(rnd() * 3);
    for (let seg = 0; seg < segments; seg++) {
      reflect.append(
        svgElement('rect', {
          x: (rx - 2 - rnd() * 3).toFixed(1),
          y: (top + seg * 10).toFixed(1),
          width: (4 + rnd() * 6).toFixed(1),
          height: 2,
          fill: warmOne ? TOWN_PALETTE.warm : TOWN_PALETTE.cyan,
          opacity: (0.5 - seg * 0.1).toFixed(2),
        }),
      );
    }
  }
  add(reflect);

  // The rocket's reflection, sharper than the others: it is the monument of the scene.
  for (let seg = 0; seg < 6; seg++) {
    add(
      svgElement('rect', {
        x: RX - 16 + (seg % 2 ? 4 : -4),
        y: HORIZON + 30 + seg * 11,
        width: 32,
        height: 3.4,
        fill: TOWN_PALETTE.cyan,
        opacity: (0.44 - seg * 0.06).toFixed(2),
      }),
    );
  }

  // Windows, lit last so they pass in front of the volumes.
  const lit = svgElement('g');
  windows.sort((a, b) => a.roll - b.roll);
  const litCount = Math.round(windows.length * litShare);
  windows.forEach((w, i) => {
    const on = i < litCount;
    const rect = svgElement('rect', {
      x: w.x,
      y: w.y,
      width: w.w,
      height: w.h,
      fill: on ? (w.roll < 0.18 ? TOWN_PALETTE.warmCore : TOWN_PALETTE.warm) : '#0d1720',
      opacity: on ? (inputs.reducedMotion ? 0.92 : 0) : 0.75,
    });
    if (on && !inputs.reducedMotion) {
      rect.style.animation = `town-window-on 620ms ease-out ${(200 + (i / Math.max(1, litCount)) * 1500).toFixed(0)}ms forwards`;
    }
    lit.append(rect);
  });
  add(lit);

  add(
    svgElement('rect', {
      x: 0,
      y: 0,
      width: TOWN_WIDTH,
      height: TOWN_HEIGHT,
      fill: 'url(#town-vignette)',
    }),
  );

  svg.replaceChildren(frag);
}

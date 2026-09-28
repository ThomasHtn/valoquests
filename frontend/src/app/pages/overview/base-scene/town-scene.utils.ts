import { animate, drawShip } from '@shared/rocket/rocket-drawing.utils';
import { SHIP } from '@shared/rocket/rocket-drawing.constants';
import { createSeededRandom } from '@core/random/seeded-random.utils';
import { svgElement } from '@core/svg/svg-element.utils';
import { SkyBody, SkyState, TownSceneInputs } from './town-scene.model';
import {
  HORIZON,
  PAD_HALF,
  PLOT,
  RX,
  SHIP_SCALE,
  SIDE_FADE_OPACITY,
  SIDE_FADE_STEPS,
  SIDE_FADE_WIDTH,
  TOWN_HEIGHT,
  TOWN_PALETTE,
  TOWN_SEED,
  TOWN_WIDTH,
} from './town-scene.constants';
import { buildingAt, growthOf, planCity, tierAt } from './city-plan.utils';
import { hourOf, mixColor, moonAt, skyAt, sunAt } from './sky-cycle.utils';
import { drawClouds, drawMoon, drawRidge, drawStars, drawSun } from './sky-drawing.utils';
import { drawBuilding } from './building-drawing.utils';

/**
 * Builds the base and its rocket as SVG nodes, under the light of the viewer's hour.
 *
 * The state of the campaign, drawn: the city is the score, the rocket gains a stage per guardian
 * defeated. Drawn imperatively into one `<svg>`
 * rather than templated: a few hundred nodes computed from a handful of numbers are a drawing, not
 * a view.
 *
 * Pure DOM construction with no Angular dependency, kept apart from the component so the drawing
 * can be read as a drawing.
 */

/**
 * The lots of the city, planned once: the plan does not depend on any input.
 */
const CITY = planCity();

/**
 * Drawings made so far, suffixing the ids of each one's gradients: two scenes on a page must not
 * share a `url(#…)`.
 */
let sceneSerial = 0;

/**
 * Draws the whole scene into an empty `<svg>`.
 *
 * @param svg - The element to fill; anything it holds is replaced.
 * @param inputs - What the scene is drawn from.
 */
export function buildTownScene(svg: SVGSVGElement, inputs: TownSceneInputs): void {
  const hour = hourOf(inputs.now);
  const sky = skyAt(hour);
  const sun = sunAt(hour);
  const moon = moonAt(hour);
  const stagesDone = Math.max(0, Math.min(SHIP.length - 1, inputs.stagesDone));

  const serial = sceneSerial++;
  const id = (name: string): string => `town-${name}-${serial}`;

  const frag = document.createDocumentFragment();
  const add = <T extends Node>(node: T): T => {
    frag.appendChild(node);
    return node;
  };

  add(defs(sky, id));

  // The sky runs above the frame too: on a phone the headroom is shown.
  add(
    svgElement('rect', {
      x: 0,
      y: -60,
      width: TOWN_WIDTH,
      height: HORIZON + 60,
      fill: `url(#${id('sky')})`,
    }),
  );
  add(drawStars(sky));
  if (moon) {
    add(drawMoon(moon, id('moon-cut')));
  }
  if (sun) {
    add(drawSun(sun));
  }
  add(drawClouds(sky, inputs.now, inputs.reducedMotion));
  add(drawRidge(sky));
  add(
    svgElement('rect', {
      x: 0,
      y: HORIZON - 1,
      width: TOWN_WIDTH,
      height: 8,
      fill: mixColor(TOWN_PALETTE.quayFace, sky.wall, 0.5),
    }),
  );

  add(drawCity(inputs, sky));
  add(drawPad(sky));

  // The pad's ground, the scale, and the vertical axis flipped so the rocket builds upward.
  const built = svgElement('g', {
    transform: `translate(${RX} ${HORIZON - 4}) scale(${SHIP_SCALE} ${-SHIP_SCALE})`,
  });
  built.append(drawShip(stagesDone));
  add(built);
  if (stagesDone > 0) {
    add(drawVapor(SHIP[stagesDone].w * SHIP_SCALE, inputs.reducedMotion));
  }

  add(drawWater(sky, sun, moon, id('sea')));

  // A light fade on both sides, so the frame eases into the page without hiding the city.
  add(
    svgElement('rect', {
      x: 0,
      y: -60,
      width: TOWN_WIDTH,
      height: TOWN_HEIGHT + 60,
      fill: `url(#${id('vignette')})`,
    }),
  );

  svg.replaceChildren(frag);
}

/**
 * Gradients of the sky, the sea and the side fade.
 */
function defs(sky: SkyState, id: (name: string) => string): SVGDefsElement {
  const defs = svgElement('defs');
  const skyGradient = svgElement('linearGradient', { id: id('sky'), x1: 0, y1: 0, x2: 0, y2: 1 });
  skyGradient.append(
    svgElement('stop', { offset: 0, 'stop-color': sky.skyTop }),
    svgElement('stop', { offset: 0.62, 'stop-color': sky.skyMid }),
    svgElement('stop', { offset: 1, 'stop-color': sky.skyLow }),
  );

  // The sea mirrors the sky under the quay, and always ends dark: the figures stand on it.
  const sea = svgElement('linearGradient', { id: id('sea'), x1: 0, y1: 0, x2: 0, y2: 1 });
  sea.append(
    svgElement('stop', { offset: 0, 'stop-color': sky.sea }),
    svgElement('stop', { offset: 0.4, 'stop-color': '#081820' }),
    svgElement('stop', { offset: 1, 'stop-color': TOWN_PALETTE.seaDeep }),
  );

  const vignette = svgElement('linearGradient', {
    id: id('vignette'),
    x1: 0,
    y1: 0,
    x2: 1,
    y2: 0,
  });
  // Eased rather than linear: a straight ramp leaves a visible line where it stops.
  const fade = (offset: number, t: number): SVGStopElement =>
    svgElement('stop', {
      offset: offset.toFixed(4),
      'stop-color': TOWN_PALETTE.night,
      'stop-opacity': (SIDE_FADE_OPACITY * (1 - t) ** 3).toFixed(3),
    });
  for (let i = 0; i <= SIDE_FADE_STEPS; i++) {
    vignette.append(fade((i / SIDE_FADE_STEPS) * SIDE_FADE_WIDTH, i / SIDE_FADE_STEPS));
  }
  for (let i = SIDE_FADE_STEPS; i >= 0; i--) {
    vignette.append(fade(1 - (i / SIDE_FADE_STEPS) * SIDE_FADE_WIDTH, i / SIDE_FADE_STEPS));
  }
  defs.append(skyGradient, sea, vignette);
  return defs;
}

/**
 * The city at the current population, back row first; what was built since the previous drawing
 * rises out of the ground, one building after the other.
 */
function drawCity(inputs: TownSceneInputs, sky: SkyState): SVGGElement {
  const growth = growthOf(inputs.population, inputs.fullCampaignPopulation);
  const before = growthOf(inputs.previousPopulation, inputs.fullCampaignPopulation);
  const city = svgElement('g');

  const rising = CITY.filter((lot) => tierAt(lot, growth) > tierAt(lot, before)).length;
  const stagger = Math.min(160, 2400 / Math.max(1, rising));
  let rank = 0;

  for (const lot of CITY) {
    const tier = tierAt(lot, growth);
    if (tier < 0) {
      continue;
    }
    const shape = buildingAt(lot, tier);
    const building = drawBuilding(lot, shape, sky, inputs.reducedMotion);

    const previousTier = tierAt(lot, before);
    if (!inputs.reducedMotion && tier > previousTier) {
      const from = previousTier < 0 ? 0 : buildingAt(lot, previousTier).h / shape.h;
      building.style.setProperty('--rise-from', from.toFixed(3));
      building.style.transformBox = 'fill-box';
      building.style.transformOrigin = '50% 100%';
      building.style.animation = `town-rise 1100ms cubic-bezier(0.22, 1, 0.36, 1) ${(400 + rank * stagger).toFixed(0)}ms both`;
      rank++;
    }
    city.append(building);
  }
  return city;
}

/**
 * The launch pad under the rocket, its two light masts lit with the street.
 */
function drawPad(sky: SkyState): SVGGElement {
  const pad = svgElement('g');
  pad.append(
    svgElement('rect', {
      x: RX - PAD_HALF,
      y: HORIZON - 5,
      width: PAD_HALF * 2,
      height: 12,
      fill: mixColor(TOWN_PALETTE.padDeck, sky.wall, 0.4),
    }),
    svgElement('rect', {
      x: RX - PAD_HALF,
      y: HORIZON - 6,
      width: PAD_HALF * 2,
      height: 1.5,
      fill: TOWN_PALETTE.padEdge,
    }),
  );
  for (const lx of [RX - PAD_HALF + 10, RX + PAD_HALF - 12]) {
    pad.append(...lamp(lx, HORIZON - 5, 30, 3, sky));
  }
  return pad;
}

/**
 * Vapour venting at the foot of a fuelled rocket: flat rounded puffs drifting away from the skirt.
 *
 * @param half - Half-width of the hull in the scene, in viewBox units.
 */
function drawVapor(half: number, reducedMotion: boolean): SVGGElement {
  const g = svgElement('g');
  const puffs = [
    { x: 4, y: -9, w: 18, drift: 10, dur: 5.2 },
    { x: 14, y: -5, w: 26, drift: 16, dur: 6.8 },
    { x: 2, y: -3, w: 14, drift: 8, dur: 4.4 },
  ];
  for (const dir of [-1, 1]) {
    puffs.forEach((puff, i) => {
      const x = dir < 0 ? RX - half - puff.x - puff.w : RX + half + puff.x;
      const bar = svgElement('rect', {
        x: x.toFixed(1),
        y: HORIZON + puff.y,
        width: puff.w,
        height: 5,
        rx: 2.5,
        fill: TOWN_PALETTE.vapor,
        opacity: 0.55,
      });
      if (!reducedMotion) {
        const begin = `-${(i * 1.7 + (dir > 0 ? 0.9 : 0)).toFixed(1)}s`;
        bar.append(
          svgElement('animateTransform', {
            attributeName: 'transform',
            type: 'translate',
            values: `0 0;${dir * puff.drift} -2`,
            dur: `${puff.dur}s`,
            begin,
            repeatCount: 'indefinite',
          }),
          animate('0;0.6;0', `${puff.dur}s`, begin),
        );
      }
      g.append(bar);
    });
  }
  return g;
}

/**
 * A lamp standing on `ground`: a foot, a mast, a bulb lit with the evening and a flat halo.
 */
function lamp(
  x: number,
  ground: number,
  height: number,
  bulb: number,
  sky: SkyState,
): SVGElement[] {
  const on = sky.lamps > 0.35;
  const nodes: SVGElement[] = [
    svgElement('rect', { x: x - 1.5, y: ground - 2, width: 5, height: 2, fill: TOWN_PALETTE.mast }),
    svgElement('rect', { x, y: ground - height, width: 2, height, fill: TOWN_PALETTE.mast }),
    svgElement('circle', {
      cx: x + 1,
      cy: ground - height - 2,
      r: bulb,
      fill: on ? TOWN_PALETTE.warmCore : TOWN_PALETTE.lampOff,
    }),
  ];
  if (on) {
    nodes.push(
      svgElement('circle', {
        cx: x + 1,
        cy: ground - height - 2,
        r: bulb * 4,
        fill: TOWN_PALETTE.warm,
        opacity: (0.14 * sky.lamps).toFixed(3),
      }),
    );
  }
  return nodes;
}

/**
 * Quay, water, lamps and reflections.
 */
function drawWater(
  sky: SkyState,
  sun: SkyBody | null,
  moon: SkyBody | null,
  seaId: string,
): SVGGElement {
  const g = svgElement('g');
  g.append(
    svgElement('rect', {
      x: 0,
      y: HORIZON + 6,
      width: TOWN_WIDTH,
      height: 7,
      fill: mixColor(TOWN_PALETTE.quayEdge, sky.wallLit, 0.45),
    }),
    svgElement('rect', {
      x: 0,
      y: HORIZON + 13,
      width: TOWN_WIDTH,
      height: 13,
      fill: mixColor(TOWN_PALETTE.quayFace, sky.wall, 0.4),
    }),
    svgElement('rect', {
      x: 0,
      y: HORIZON + 26,
      width: TOWN_WIDTH,
      height: TOWN_HEIGHT - HORIZON - 26,
      fill: `url(#${seaId})`,
    }),
  );

  for (let lx = 44; lx < TOWN_WIDTH; lx += 122) {
    if (lx > PLOT[0] - 40 && lx < PLOT[1] + 40) {
      continue;
    }
    g.append(...lamp(lx, HORIZON + 8, 26, 2.6, sky));
  }

  // Reflections: broken columns, never continuous streaks; warm lights by night, sky glints by day.
  const rnd = createSeededRandom(TOWN_SEED + 4);
  const night = sky.lamps > 0.35;
  const reflect = svgElement('g', { opacity: night ? 0.5 : 0.32 });
  for (let i = 0; i < 40; i++) {
    const rx = rnd() * TOWN_WIDTH;
    const top = HORIZON + 28 + rnd() * 14;
    const warmOne = rnd() < 0.72;
    const segments = 2 + Math.floor(rnd() * 3);
    const fill = !night ? TOWN_PALETTE.dayGlint : warmOne ? TOWN_PALETTE.warm : TOWN_PALETTE.cyan;
    for (let seg = 0; seg < segments; seg++) {
      reflect.append(
        svgElement('rect', {
          x: (rx - 2 - rnd() * 3).toFixed(1),
          y: (top + seg * 10).toFixed(1),
          width: (4 + rnd() * 6).toFixed(1),
          height: 2,
          fill,
          opacity: (0.5 - seg * 0.1).toFixed(2),
        }),
      );
    }
  }
  g.append(reflect);

  // The sun or the moon laid on the water, widest when it is low.
  const body = sun ?? moon;
  if (body) {
    const fill = sun
      ? mixColor(TOWN_PALETTE.sunLow, TOWN_PALETTE.sunHigh, sun.elevation)
      : TOWN_PALETTE.moon;
    for (let seg = 0; seg < 5; seg++) {
      const w = (26 - seg * 3) * (1.3 - body.elevation * 0.6);
      g.append(
        svgElement('rect', {
          x: (body.x - w / 2 + (seg % 2 ? 3 : -3)).toFixed(1),
          y: HORIZON + 30 + seg * 12,
          width: w.toFixed(1),
          height: 2.4,
          fill,
          opacity: ((0.42 - seg * 0.07) * (1 - body.elevation * 0.4)).toFixed(2),
        }),
      );
    }
  }

  // The rocket's reflection, sharper than the others: it is the monument of the scene.
  for (let seg = 0; seg < 5; seg++) {
    g.append(
      svgElement('rect', {
        x: RX - 12 + (seg % 2 ? 3 : -3),
        y: HORIZON + 30 + seg * 12,
        width: 24,
        height: 3,
        fill: TOWN_PALETTE.cyan,
        opacity: (0.44 - seg * 0.07).toFixed(2),
      }),
    );
  }
  return g;
}

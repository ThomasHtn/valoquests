import { createSeededRandom } from '@core/random/seeded-random.utils';
import { svgElement } from '@core/svg/svg-element.utils';
import { SHIP } from '@shared/rocket/rocket-drawing.constants';
import { animate, drawShip } from '@shared/rocket/rocket-drawing.utils';
import { drawBuilding } from './town-building.utils';
import { buildingAt, growthOf, tierAt } from './town-plan.utils';
import {
  CITY,
  HORIZON,
  PAD_HALF,
  PLOT,
  QUAY_LAMP_CLEARANCE,
  QUAY_LAMP_SPACING,
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
import { SkyBody, SkyState, TownSceneInputs } from './town-scene.model';
import { hourOf, mixColor, moonAt, skyAt, sunAt } from './town-sky-cycle.utils';
import { drawClouds, drawMoon, drawRidge, drawStars, drawSun } from './town-sky.utils';

/**
 * Plain SVG nodes built outside Angular.
 */

/**
 * Drawing counter suffixing gradient ids, so two scenes on a page never share a `url(#…)`.
 */
let sceneSerial = 0;

/**
 * Draws the whole scene into `svg`, replacing its content.
 */
export function buildTownScene(svg: SVGSVGElement, inputs: TownSceneInputs): void {
  const hour = hourOf(inputs.now);
  const sky = skyAt(hour);
  const sun = sunAt(hour);
  const moon = moonAt(hour);
  const id = nextSceneIds();

  // Back to front.
  const frag = document.createDocumentFragment();
  frag.append(
    defs(sky, id),
    ...drawSky(sky, sun, moon, inputs, id),
    drawCity(inputs, sky),
    drawPad(sky),
    ...drawRocket(inputs),
    drawWater(sky, sun, moon, id('sea')),
    drawSideFade(id),
  );
  svg.replaceChildren(frag);
}

/**
 * Id maker suffixing each gradient id with a new drawing serial.
 */
function nextSceneIds(): (name: string) => string {
  const serial = sceneSerial++;
  return (name) => `town-${name}-${serial}`;
}

/**
 * Everything behind the city: sky, sun or moon, clouds, hills and the strip at their foot.
 */
function drawSky(
  sky: SkyState,
  sun: SkyBody | null,
  moon: SkyBody | null,
  inputs: TownSceneInputs,
  id: (name: string) => string,
): SVGElement[] {
  // Extends above the frame: phones show the headroom.
  const layers: SVGElement[] = [
    svgElement('rect', {
      x: 0,
      y: -60,
      width: TOWN_WIDTH,
      height: HORIZON + 60,
      fill: `url(#${id('sky')})`,
    }),
    drawStars(sky),
  ];
  if (moon) {
    layers.push(drawMoon(moon, id('moon-cut')));
  }
  if (sun) {
    layers.push(drawSun(sun));
  }
  layers.push(
    drawClouds(sky, inputs.now, inputs.reducedMotion),
    drawRidge(sky),
    svgElement('rect', {
      x: 0,
      y: HORIZON - 1,
      width: TOWN_WIDTH,
      height: 8,
      fill: mixColor(TOWN_PALETTE.quayFace, sky.wall, 0.5),
    }),
  );
  return layers;
}

/**
 * Rocket at its current stage, venting vapour once a stage stands.
 */
function drawRocket(inputs: TownSceneInputs): SVGElement[] {
  const stagesDone = Math.max(0, Math.min(SHIP.length - 1, inputs.stagesDone));

  // Y axis flipped so the rocket builds upward.
  const built = svgElement('g', {
    transform: `translate(${RX} ${HORIZON - 4}) scale(${SHIP_SCALE} ${-SHIP_SCALE})`,
  });
  built.append(drawShip(stagesDone));
  if (stagesDone === 0) {
    return [built];
  }
  return [built, drawVapor(SHIP[stagesDone].w * SHIP_SCALE, inputs.reducedMotion)];
}

/**
 * Light fade on both sides, easing the frame into the page.
 */
function drawSideFade(id: (name: string) => string): SVGRectElement {
  return svgElement('rect', {
    x: 0,
    y: -60,
    width: TOWN_WIDTH,
    height: TOWN_HEIGHT + 60,
    fill: `url(#${id('vignette')})`,
  });
}

/**
 * Gradients of the sky, the sea and the side fade.
 */
function defs(sky: SkyState, id: (name: string) => string): SVGDefsElement {
  const defs = svgElement('defs');
  defs.append(skyGradient(sky, id), seaGradient(sky, id), sideFadeGradient(id));
  return defs;
}

/**
 * Vertical sky gradient down to the horizon.
 */
function skyGradient(sky: SkyState, id: (name: string) => string): SVGLinearGradientElement {
  const gradient = svgElement('linearGradient', { id: id('sky'), x1: 0, y1: 0, x2: 0, y2: 1 });
  gradient.append(
    svgElement('stop', { offset: 0, 'stop-color': sky.skyTop }),
    svgElement('stop', { offset: 0.62, 'stop-color': sky.skyMid }),
    svgElement('stop', { offset: 1, 'stop-color': sky.skyLow }),
  );
  return gradient;
}

/**
 * Sea mirroring the sky under the quay, always ending dark since figures stand on it.
 */
function seaGradient(sky: SkyState, id: (name: string) => string): SVGLinearGradientElement {
  const gradient = svgElement('linearGradient', { id: id('sea'), x1: 0, y1: 0, x2: 0, y2: 1 });
  gradient.append(
    svgElement('stop', { offset: 0, 'stop-color': sky.sea }),
    svgElement('stop', { offset: 0.4, 'stop-color': '#081820' }),
    svgElement('stop', { offset: 1, 'stop-color': TOWN_PALETTE.seaDeep }),
  );
  return gradient;
}

/**
 * Horizontal gradient darkening both edges of the frame, transparent in between.
 */
function sideFadeGradient(id: (name: string) => string): SVGLinearGradientElement {
  const gradient = svgElement('linearGradient', {
    id: id('vignette'),
    x1: 0,
    y1: 0,
    x2: 1,
    y2: 0,
  });
  // Left edge inward, then right edge outward, so offsets stay increasing.
  for (let i = 0; i <= SIDE_FADE_STEPS; i++) {
    const t = i / SIDE_FADE_STEPS;
    gradient.append(sideFadeStop(t * SIDE_FADE_WIDTH, t));
  }
  for (let i = SIDE_FADE_STEPS; i >= 0; i--) {
    const t = i / SIDE_FADE_STEPS;
    gradient.append(sideFadeStop(1 - t * SIDE_FADE_WIDTH, t));
  }
  return gradient;
}

/**
 * Side fade stop, `t` being its distance from the edge as a share of the fade.
 */
function sideFadeStop(offset: number, t: number): SVGStopElement {
  // Eased: a linear ramp leaves a visible line where it stops.
  return svgElement('stop', {
    offset: offset.toFixed(4),
    'stop-color': TOWN_PALETTE.night,
    'stop-opacity': (SIDE_FADE_OPACITY * (1 - t) ** 3).toFixed(3),
  });
}

/**
 * City at the current population; buildings grown since the previous drawing rise in turn.
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
 * Launch pad with two light masts lit with the street.
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
 * Vapour puffs drifting from the rocket's foot; `half` is the hull half-width in viewBox units.
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
 * Lamp standing on `ground`, its bulb and flat halo lit in the evening.
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
  g.append(...drawQuay(sky, seaId), ...drawQuayLamps(sky), drawLightReflections(sky));
  const body = sun ?? moon;
  if (body) {
    const fill = sun
      ? mixColor(TOWN_PALETTE.sunLow, TOWN_PALETTE.sunHigh, sun.elevation)
      : TOWN_PALETTE.moon;
    g.append(...drawBodyReflection(body, fill));
  }
  g.append(...drawRocketReflection());
  return g;
}

/**
 * Quay edge and face, then the sea below.
 */
function drawQuay(sky: SkyState, seaId: string): SVGRectElement[] {
  return [
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
  ];
}

/**
 * Lamps evenly spaced along the quay, none in front of the launch plot.
 */
function drawQuayLamps(sky: SkyState): SVGElement[] {
  const nodes: SVGElement[] = [];
  for (let lx = 44; lx < TOWN_WIDTH; lx += QUAY_LAMP_SPACING) {
    const facesPlot = lx > PLOT[0] - QUAY_LAMP_CLEARANCE && lx < PLOT[1] + QUAY_LAMP_CLEARANCE;
    if (facesPlot) {
      continue;
    }
    nodes.push(...lamp(lx, HORIZON + 8, 26, 2.6, sky));
  }
  return nodes;
}

/**
 * Light reflections as broken columns: warm lights by night, sky glints by day.
 */
function drawLightReflections(sky: SkyState): SVGGElement {
  // Seeded so reflections stay still across drawings: keep the draw order.
  const rnd = createSeededRandom(TOWN_SEED + 4);
  const night = sky.lamps > 0.35;
  const reflect = svgElement('g', { opacity: night ? 0.5 : 0.32 });
  for (let i = 0; i < 40; i++) {
    const rx = rnd() * TOWN_WIDTH;
    const top = HORIZON + 28 + rnd() * 14;
    const warmOne = rnd() < 0.72;
    const segments = 2 + Math.floor(rnd() * 3);
    const fill = reflectionColor(night, warmOne);
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
  return reflect;
}

/**
 * Reflection colour: sky glint by day, warm or cyan light by night.
 */
function reflectionColor(night: boolean, warmOne: boolean): string {
  if (!night) {
    return TOWN_PALETTE.dayGlint;
  }
  return warmOne ? TOWN_PALETTE.warm : TOWN_PALETTE.cyan;
}

/**
 * Sun or moon on the water, widest when low.
 */
function drawBodyReflection(body: SkyBody, fill: string): SVGRectElement[] {
  const segments: SVGRectElement[] = [];
  for (let seg = 0; seg < 5; seg++) {
    const w = (26 - seg * 3) * (1.3 - body.elevation * 0.6);
    segments.push(
      svgElement('rect', {
        x: (body.x - w / 2 + reflectionJog(seg)).toFixed(1),
        y: HORIZON + 30 + seg * 12,
        width: w.toFixed(1),
        height: 2.4,
        fill,
        opacity: ((0.42 - seg * 0.07) * (1 - body.elevation * 0.4)).toFixed(2),
      }),
    );
  }
  return segments;
}

/**
 * Rocket reflection, sharper than the others as the scene's centrepiece.
 */
function drawRocketReflection(): SVGRectElement[] {
  const segments: SVGRectElement[] = [];
  for (let seg = 0; seg < 5; seg++) {
    segments.push(
      svgElement('rect', {
        x: RX - 12 + reflectionJog(seg),
        y: HORIZON + 30 + seg * 12,
        width: 24,
        height: 3,
        fill: TOWN_PALETTE.cyan,
        opacity: (0.44 - seg * 0.07).toFixed(2),
      }),
    );
  }
  return segments;
}

/**
 * Alternating sideways shift so a reflection column looks broken by waves.
 */
function reflectionJog(seg: number): number {
  return seg % 2 ? 3 : -3;
}

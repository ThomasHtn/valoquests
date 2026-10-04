import { svgElement } from '@core/svg/svg-element.utils';
import { createSeededRandom } from '@core/random/seeded-random.utils';
import { SkyBody, SkyState } from './town-scene.model';
import {
  CLOUD_CROSSING_S,
  CLOUD_RANGE,
  HORIZON,
  TOWN_PALETTE,
  TOWN_SEED,
  TOWN_WIDTH,
} from './town-scene.constants';
import { mixColor } from './town-sky-cycle.utils';

/**
 * Flat layers above the city: solid tints, no light wash.
 */

/**
 * Stars: a fixed sequence faded (not removed) as the sky brightens, so dawn thins them.
 */
export function drawStars(sky: SkyState): SVGGElement {
  const g = svgElement('g', { opacity: sky.stars.toFixed(2) });
  if (sky.stars <= 0.01) {
    return g;
  }
  const rnd = createSeededRandom(TOWN_SEED);
  for (let i = 0; i < 60; i++) {
    g.append(
      svgElement('circle', {
        cx: (rnd() * TOWN_WIDTH).toFixed(1),
        cy: (-12 + rnd() * (HORIZON - 70)).toFixed(1),
        r: rnd() < 0.12 ? 1.5 : 0.9,
        fill: '#cfe4ee',
        opacity: (0.35 + rnd() * 0.5).toFixed(2),
      }),
    );
  }
  return g;
}

/**
 * Sun: a flat disc, orange near the horizon, pale at its height.
 */
export function drawSun(sun: SkyBody): SVGCircleElement {
  return svgElement('circle', {
    cx: sun.x.toFixed(1),
    cy: sun.y.toFixed(1),
    r: (13 - sun.elevation * 3).toFixed(1),
    fill: mixColor(TOWN_PALETTE.sunLow, TOWN_PALETTE.sunHigh, Math.min(1, sun.elevation * 1.6)),
  });
}

/**
 * Moon: a crescent cut by a mask, so the sky shows through whatever its colour.
 */
export function drawMoon(moon: SkyBody, maskId: string): SVGGElement {
  const g = svgElement('g');
  const mask = svgElement('mask', { id: maskId });
  mask.append(
    svgElement('rect', { x: moon.x - 12, y: moon.y - 12, width: 24, height: 24, fill: '#fff' }),
    svgElement('circle', { cx: moon.x + 4.5, cy: moon.y - 2.5, r: 8, fill: '#000' }),
  );
  g.append(
    mask,
    svgElement('circle', {
      cx: moon.x.toFixed(1),
      cy: moon.y.toFixed(1),
      r: 9,
      fill: TOWN_PALETTE.moon,
      mask: `url(#${maskId})`,
    }),
  );
  return g;
}

/**
 * Clouds drifting left to right: weather seeded by the date, position read from the clock.
 */
export function drawClouds(sky: SkyState, now: number, reducedMotion: boolean): SVGGElement {
  // One random sequence feeds every draw below: keep the call order.
  const rnd = weatherOfTheDay(now);
  const count = CLOUD_RANGE[0] + Math.floor(rnd() * (CLOUD_RANGE[1] - CLOUD_RANGE[0] + 1));
  const g = svgElement('g', { opacity: 0.94 });
  for (let i = 0; i < count; i++) {
    g.append(drawCloud(rnd, sky, now, reducedMotion));
  }
  return g;
}

/**
 * Random sequence seeded by the local date, the same all day long.
 */
function weatherOfTheDay(now: number): () => number {
  const date = new Date(now);
  return createSeededRandom(
    TOWN_SEED + date.getFullYear() * 10_000 + (date.getMonth() + 1) * 100 + date.getDate(),
  );
}

/**
 * One cloud: a shaded base with one to three shorter bars piled on it.
 */
function drawCloud(
  rnd: () => number,
  sky: SkyState,
  now: number,
  reducedMotion: boolean,
): SVGGElement {
  const y = 6 + rnd() * (HORIZON - 120);
  const width = 60 + rnd() * 120;
  const barH = 7 + rnd() * 3;
  const cloud = svgElement('g');
  cloud.append(...cloudBase(sky, width, barH), ...cloudTops(rnd, sky, width, barH));
  setCloudDrift(cloud, rnd, now, width, y, reducedMotion);
  return cloud;
}

/**
 * Shaded bottom bar, then a shorter body so the underside shows as a flat strip.
 */
function cloudBase(sky: SkyState, width: number, barH: number): SVGRectElement[] {
  return [
    svgElement('rect', {
      x: 0,
      y: 0,
      width: width.toFixed(1),
      height: barH,
      rx: barH / 2,
      fill: sky.cloudShade,
    }),
    svgElement('rect', {
      x: 0,
      y: 0,
      width: width.toFixed(1),
      height: barH - 2.5,
      rx: (barH - 2.5) / 2,
      fill: sky.cloud,
    }),
  ];
}

/**
 * Bars piled on the base, each shorter than the one below, shifted at random.
 */
function cloudTops(
  rnd: () => number,
  sky: SkyState,
  width: number,
  barH: number,
): SVGRectElement[] {
  const tops: SVGRectElement[] = [];
  const bars = 1 + Math.floor(rnd() * 3);
  for (let bar = 1; bar <= bars; bar++) {
    const w = width * (0.82 - bar * 0.2 - rnd() * 0.12);
    tops.push(
      svgElement('rect', {
        x: (rnd() * (width - w)).toFixed(1),
        y: (-bar * (barH - 2)).toFixed(1),
        width: w.toFixed(1),
        height: barH,
        rx: barH / 2,
        fill: sky.cloud,
      }),
    );
  }
  return tops;
}

/**
 * Places a cloud on its crossing from the clock: frozen under reduced motion, else animated.
 */
function setCloudDrift(
  cloud: SVGGElement,
  rnd: () => number,
  now: number,
  width: number,
  y: number,
  reducedMotion: boolean,
): void {
  const crossing = CLOUD_CROSSING_S[0] + rnd() * (CLOUD_CROSSING_S[1] - CLOUD_CROSSING_S[0]);
  const phase = (now / 1000 / crossing + rnd()) % 1;
  const from = -width - 20;
  const to = TOWN_WIDTH + 20;
  if (reducedMotion) {
    cloud.setAttribute(
      'transform',
      `translate(${(from + (to - from) * phase).toFixed(1)} ${y.toFixed(1)})`,
    );
    return;
  }
  cloud.append(
    svgElement('animateTransform', {
      attributeName: 'transform',
      type: 'translate',
      values: `${from.toFixed(1)} ${y.toFixed(1)};${to} ${y.toFixed(1)}`,
      dur: `${crossing.toFixed(0)}s`,
      begin: `-${(phase * crossing).toFixed(0)}s`,
      repeatCount: 'indefinite',
    }),
  );
}

/**
 * Hills behind the base: two soft ridges in close tones.
 */
export function drawRidge(sky: SkyState): SVGGElement {
  const rnd = createSeededRandom(TOWN_SEED + 3);
  const g = svgElement('g');
  g.append(
    svgElement('path', {
      d: rolling(rnd, [110, 190], [40, 72]),
      fill: mixColor(sky.ridge, sky.skyLow, 0.16),
    }),
    svgElement('path', { d: rolling(rnd, [90, 150], [16, 38]), fill: sky.ridge }),
  );
  return g;
}

/**
 * Closed path of rounded hills: random summits joined through midpoints so no crest is pointed.
 */
function rolling(
  rnd: () => number,
  [minStep, maxStep]: readonly [number, number],
  [minH, maxH]: readonly [number, number],
): string {
  const peaks: [number, number][] = [];
  for (let x = -80; x <= TOWN_WIDTH + 160; x += minStep + rnd() * (maxStep - minStep)) {
    peaks.push([x, HORIZON - minH - rnd() * (maxH - minH)]);
  }
  let d = `M${peaks[0][0].toFixed(0)} ${HORIZON} L${peaks[0][0].toFixed(0)} ${peaks[0][1].toFixed(0)}`;
  for (let i = 1; i < peaks.length; i++) {
    const [px, py] = peaks[i - 1];
    const [x, y] = peaks[i];
    d += ` Q${px.toFixed(0)} ${py.toFixed(0)} ${((px + x) / 2).toFixed(0)} ${((py + y) / 2).toFixed(0)}`;
  }
  const [lx, ly] = peaks[peaks.length - 1];
  return `${d} L${lx.toFixed(0)} ${ly.toFixed(0)} L${lx.toFixed(0)} ${HORIZON} Z`;
}

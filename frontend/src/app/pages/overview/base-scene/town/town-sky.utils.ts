import { createSeededRandom } from '@core/random/seeded-random.utils';
import { svgElement } from '@core/svg/svg-element.utils';

import {
  CLOUD_CROSSING_S,
  CLOUD_RANGE,
  HORIZON,
  TOWN_PALETTE,
  TOWN_SEED,
  TOWN_WIDTH,
} from './town-scene.constants';
import { SkyBody, SkyState } from './town-scene.model';
import { mixColor } from './town-sky-cycle.utils';

/**
 * Flat layers above the city: solid tints, no light wash.
 */

/**
 * Stars: a fixed sequence faded (not removed) as the sky brightens, so dawn thins them.
 */
export function drawStars(sky: SkyState): SVGGElement {
  const stars = svgElement('g', { opacity: sky.stars.toFixed(2) });
  if (sky.stars <= 0.01) {
    return stars;
  }
  const random = createSeededRandom(TOWN_SEED);
  for (let i = 0; i < 60; i++) {
    stars.append(
      svgElement('circle', {
        cx: (random() * TOWN_WIDTH).toFixed(1),
        cy: (-12 + random() * (HORIZON - 70)).toFixed(1),
        r: random() < 0.12 ? 1.5 : 0.9,
        fill: TOWN_PALETTE.star,
        opacity: (0.35 + random() * 0.5).toFixed(2),
      }),
    );
  }
  return stars;
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
  const moonGroup = svgElement('g');
  const mask = svgElement('mask', { id: maskId });
  mask.append(
    svgElement('rect', { x: moon.x - 12, y: moon.y - 12, width: 24, height: 24, fill: '#fff' }),
    svgElement('circle', { cx: moon.x + 4.5, cy: moon.y - 2.5, r: 8, fill: '#000' }),
  );
  moonGroup.append(
    mask,
    svgElement('circle', {
      cx: moon.x.toFixed(1),
      cy: moon.y.toFixed(1),
      r: 9,
      fill: TOWN_PALETTE.moon,
      mask: `url(#${maskId})`,
    }),
  );
  return moonGroup;
}

/**
 * Clouds drifting left to right: weather seeded by the date, position read from the clock.
 */
export function drawClouds(sky: SkyState, now: number, reducedMotion: boolean): SVGGElement {
  // One random sequence feeds every draw below: keep the call order.
  const random = weatherOfTheDay(now);
  const count = CLOUD_RANGE[0] + Math.floor(random() * (CLOUD_RANGE[1] - CLOUD_RANGE[0] + 1));
  const clouds = svgElement('g', { opacity: 0.94 });
  for (let i = 0; i < count; i++) {
    clouds.append(drawCloud(random, sky, now, reducedMotion));
  }
  return clouds;
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
  random: () => number,
  sky: SkyState,
  now: number,
  reducedMotion: boolean,
): SVGGElement {
  const y = 6 + random() * (HORIZON - 120);
  const width = 60 + random() * 120;
  const barH = 7 + random() * 3;
  const cloud = svgElement('g');
  cloud.append(...cloudBase(sky, width, barH), ...cloudTops(random, sky, width, barH));
  setCloudDrift(cloud, random, now, width, y, reducedMotion);
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
  random: () => number,
  sky: SkyState,
  width: number,
  barH: number,
): SVGRectElement[] {
  const tops: SVGRectElement[] = [];
  const bars = 1 + Math.floor(random() * 3);
  for (let bar = 1; bar <= bars; bar++) {
    const w = width * (0.82 - bar * 0.2 - random() * 0.12);
    tops.push(
      svgElement('rect', {
        x: (random() * (width - w)).toFixed(1),
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
  random: () => number,
  now: number,
  width: number,
  y: number,
  reducedMotion: boolean,
): void {
  const crossing = CLOUD_CROSSING_S[0] + random() * (CLOUD_CROSSING_S[1] - CLOUD_CROSSING_S[0]);
  const phase = (now / 1000 / crossing + random()) % 1;
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
  const random = createSeededRandom(TOWN_SEED + 3);
  const ridges = svgElement('g');
  ridges.append(
    svgElement('path', {
      d: rollingHillsPath(random, [110, 190], [40, 72]),
      fill: mixColor(sky.ridge, sky.skyLow, 0.16),
    }),
    svgElement('path', { d: rollingHillsPath(random, [90, 150], [16, 38]), fill: sky.ridge }),
  );
  return ridges;
}

/**
 * Closed path of rounded hills: random summits joined through midpoints so no crest is pointed.
 */
function rollingHillsPath(
  random: () => number,
  [minStep, maxStep]: readonly [number, number],
  [minH, maxH]: readonly [number, number],
): string {
  const peaks: [number, number][] = [];
  for (let x = -80; x <= TOWN_WIDTH + 160; x += minStep + random() * (maxStep - minStep)) {
    peaks.push([x, HORIZON - minH - random() * (maxH - minH)]);
  }
  let d = `M${peaks[0][0].toFixed(0)} ${HORIZON} L${peaks[0][0].toFixed(0)} ${peaks[0][1].toFixed(0)}`;
  for (let i = 1; i < peaks.length; i++) {
    const [px, py] = peaks[i - 1];
    const [x, y] = peaks[i];
    d += ` Q${px.toFixed(0)} ${py.toFixed(0)} ${((px + x) / 2).toFixed(0)} ${((py + y) / 2).toFixed(0)}`;
  }
  const [lastX, lastY] = peaks[peaks.length - 1];
  return `${d} L${lastX.toFixed(0)} ${lastY.toFixed(0)} L${lastX.toFixed(0)} ${HORIZON} Z`;
}

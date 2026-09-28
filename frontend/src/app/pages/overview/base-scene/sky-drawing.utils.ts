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
import { mixColor } from './sky-cycle.utils';

/**
 * The layers above the city: stars, sun or moon, clouds and the hills behind the base.
 *
 * Everything is flat: discs and bars in solid tints, no light wash. The hour colours them, the
 * date decides the weather.
 */

/**
 * Stars: a fixed sequence, faded out as the sky brightens rather than removed, so dawn thins them
 * instead of redrawing them.
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
 * The sun: a flat disc, orange near the horizon and pale at its height.
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
 * The moon: a crescent cut by a mask, so the sky behind it stays the sky whatever its colour.
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
 * Clouds: stacks of rounded bars, the underside in shade, drifting left to right.
 *
 * The weather is drawn from the date, so a day keeps its sky between two visits and the next day
 * brings another. Their position is read from the clock, so a redraw finds them where they were.
 */
export function drawClouds(sky: SkyState, now: number, reducedMotion: boolean): SVGGElement {
  const date = new Date(now);
  const rnd = createSeededRandom(
    TOWN_SEED + date.getFullYear() * 10_000 + (date.getMonth() + 1) * 100 + date.getDate(),
  );
  const count = CLOUD_RANGE[0] + Math.floor(rnd() * (CLOUD_RANGE[1] - CLOUD_RANGE[0] + 1));
  const g = svgElement('g', { opacity: 0.94 });

  for (let i = 0; i < count; i++) {
    const y = 6 + rnd() * (HORIZON - 120);
    const width = 60 + rnd() * 120;
    const barH = 7 + rnd() * 3;
    const cloud = svgElement('g');

    // Bottom bar in shade, then the body a little shorter so the underside shows as a flat strip.
    cloud.append(
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
    );
    const bars = 1 + Math.floor(rnd() * 3);
    for (let bar = 1; bar <= bars; bar++) {
      const w = width * (0.82 - bar * 0.2 - rnd() * 0.12);
      cloud.append(
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

    const crossing = CLOUD_CROSSING_S[0] + rnd() * (CLOUD_CROSSING_S[1] - CLOUD_CROSSING_S[0]);
    const phase = (now / 1000 / crossing + rnd()) % 1;
    const from = -width - 20;
    const to = TOWN_WIDTH + 20;
    if (reducedMotion) {
      cloud.setAttribute(
        'transform',
        `translate(${(from + (to - from) * phase).toFixed(1)} ${y.toFixed(1)})`,
      );
    } else {
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
    g.append(cloud);
  }
  return g;
}

/**
 * Hills behind the base, two soft ridges in close tones: the colony starts from a landscape, not
 * from a city already standing on the horizon.
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
 * A closed ridge of rounded hills: summits drawn at random, joined by curves through the midpoints
 * between them, so no crest ends in a point.
 *
 * @param spacing - Shortest and longest distance between two summits.
 * @param heights - Lowest and highest summit above the horizon.
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

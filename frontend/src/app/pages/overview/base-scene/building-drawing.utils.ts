import { svgElement } from '@core/svg/svg-element.utils';
import { hashUnit } from '@core/random/hash-unit.utils';
import { animate } from '@shared/rocket/rocket-drawing.utils';
import { BuildingShape, BuildingVolume, Lot, SkyState } from './town-scene.model';
import {
  CONCRETE_SHARE,
  FACADE_TINTS,
  HORIZON,
  ROOF_TILE,
  TOP_TIER,
  TOWER_TIER,
  TOWN_PALETTE,
} from './town-scene.constants';
import { mixColor } from './sky-cycle.utils';

/**
 * One building per tier, drawn as flat volumes.
 *
 * Each tier is a type, not a size: a cabin and a house take a pitched roof, a small building a
 * water tank, a block a rooftop plant, a tower a setback and a mast, a skyscraper two setbacks and a
 * beacon. The city changes character as it grows, not only height.
 */

/**
 * Draws the building a lot carries.
 *
 * @param lot - The lot, whose id keys every draw so the building keeps its face across renders.
 * @param shape - The building at its current tier.
 * @param sky - Light of the hour: facade tones and the share of lit windows.
 * @param reducedMotion - Whether windows and beacons stand still.
 */
export function drawBuilding(
  lot: Lot,
  shape: BuildingShape,
  sky: SkyState,
  reducedMotion: boolean,
): SVGGElement {
  // The back row sits in the haze: a quarter of the way to the horizon's colour.
  const tone = (color: string): string =>
    lot.row === 'back' ? mixColor(color, sky.haze, 0.24) : color;
  const wall = tone(facade(lot, shape.tier, sky));
  const roof = tone(sky.roof);
  const { tier, x, w, h } = shape;
  const top = HORIZON - h;
  const g = svgElement('g');

  const volumes: BuildingVolume[] = [];
  if (tier === TOWER_TIER) {
    const cut = top + h * 0.22;
    const inset = Math.round(w * 0.14);
    volumes.push(
      { x, w, top: cut, bottom: HORIZON },
      { x: x + inset, w: w - inset * 2, top, bottom: cut },
    );
  } else if (tier === TOP_TIER) {
    const cutHigh = top + h * 0.12;
    const cutLow = top + h * 0.34;
    const inset = Math.round(w * 0.12);
    volumes.push(
      { x, w, top: cutLow, bottom: HORIZON },
      { x: x + inset, w: w - inset * 2, top: cutHigh, bottom: cutLow },
      { x: x + inset * 2.2, w: w - inset * 4.4, top, bottom: cutHigh },
    );
  } else {
    volumes.push({ x, w, top, bottom: HORIZON });
  }

  for (const volume of volumes) {
    g.append(
      svgElement('rect', {
        x: volume.x,
        y: volume.top,
        width: volume.w,
        height: volume.bottom - volume.top,
        fill: wall,
      }),
      svgElement('rect', {
        x: volume.x + volume.w - 3,
        y: volume.top,
        width: 3,
        height: volume.bottom - volume.top,
        fill: roof,
        opacity: 0.6,
      }),
    );
    if (tier >= 2) {
      g.append(
        svgElement('rect', {
          x: volume.x - 1,
          y: volume.top,
          width: volume.w + 2,
          height: 3,
          fill: roof,
        }),
      );
    }
  }

  appendRoof(g, lot, shape, roof, sky, reducedMotion);
  volumes.forEach((volume, index) =>
    appendWindows(g, lot, tier, volume, index, sky, tone, reducedMotion),
  );

  // A lit sign on some blocks, the one colour of the city that is not a window.
  if (tier === 3 && hashUnit(lot.id, 6) < 0.3) {
    g.append(
      svgElement('rect', {
        x: x + 5,
        y: top + h * 0.42,
        width: w - 13,
        height: 2.5,
        fill: TOWN_PALETTE.cyan,
        opacity: 0.75,
      }),
    );
  }
  return g;
}

/**
 * Facade colour of a lot: bare concrete for some, a paint for the others, stronger on houses than on
 * towers of glass and steel, and paler at night when colours fade.
 */
function facade(lot: Lot, tier: number, sky: SkyState): string {
  const base = hashUnit(lot.id, 5) < 0.45 ? sky.wall : sky.wallLit;
  const pick = hashUnit(lot.id, 9);
  if (pick < CONCRETE_SHARE) {
    return base;
  }
  const tint =
    FACADE_TINTS[
      Math.floor(((pick - CONCRETE_SHARE) / (1 - CONCRETE_SHARE)) * FACADE_TINTS.length)
    ];
  const strength = tier <= 1 ? 1 : tier >= TOWER_TIER ? 0.5 : 0.8;
  return mixColor(base, tint, (0.22 + 0.26 * (1 - sky.lamps)) * strength);
}

/**
 * Adds what stands on the roof: the pitch of a house, a water tank, a rooftop plant, a mast.
 */
function appendRoof(
  g: SVGGElement,
  lot: Lot,
  { tier, x, w, h }: BuildingShape,
  roof: string,
  sky: SkyState,
  reducedMotion: boolean,
): void {
  const top = HORIZON - h;
  if (tier <= 1) {
    const pitch = tier === 0 ? 6 : 9;
    g.append(
      svgElement('path', {
        d: `M${x - 2} ${top + 1} L${x + w / 2} ${top - pitch} L${x + w + 2} ${top + 1} Z`,
        fill:
          hashUnit(lot.id, 10) < 0.55
            ? mixColor(roof, ROOF_TILE, 0.3 + 0.3 * (1 - sky.lamps))
            : roof,
      }),
    );
    if (tier === 1 && hashUnit(lot.id, 7) < 0.55) {
      g.append(
        svgElement('rect', {
          x: x + w * 0.7,
          y: top - pitch + 1,
          width: 3,
          height: pitch - 1,
          fill: roof,
        }),
      );
    }
    return;
  }
  if (tier === 2) {
    const tx = x + 4 + hashUnit(lot.id, 7) * (w - 14);
    g.append(
      svgElement('rect', { x: tx + 1, y: top - 4, width: 1, height: 4, fill: roof }),
      svgElement('rect', { x: tx + 5, y: top - 4, width: 1, height: 4, fill: roof }),
      svgElement('rect', { x: tx, y: top - 10, width: 7, height: 6, fill: roof }),
    );
    return;
  }
  if (tier === 3) {
    const bw = Math.round(w * 0.32);
    g.append(
      svgElement('rect', {
        x: x + 4 + hashUnit(lot.id, 7) * (w - bw - 8),
        y: top - 6,
        width: bw,
        height: 6,
        fill: roof,
      }),
    );
    return;
  }

  // Towers and skyscrapers: a light edge on the facade, and a mast on the crown.
  g.append(
    svgElement('rect', {
      x: x + 3,
      y: top + h * 0.25,
      width: 1.5,
      height: h * 0.75,
      fill: TOWN_PALETTE.steelLit,
      opacity: 0.5,
    }),
  );
  const mast = tier === TOP_TIER ? 24 : 12;
  g.append(
    svgElement('rect', {
      x: x + w / 2 - 0.75,
      y: top - mast,
      width: 1.5,
      height: mast,
      fill: TOWN_PALETTE.mast,
    }),
  );
  if (tier === TOP_TIER) {
    const beacon = svgElement('circle', {
      cx: x + w / 2,
      cy: top - mast - 1,
      r: 1.8,
      fill: TOWN_PALETTE.red,
      opacity: 0.9,
    });
    if (!reducedMotion) {
      beacon.append(animate('0.9;0.1;0.9', `${(2 + hashUnit(lot.id, 8) * 2).toFixed(1)}s`));
    }
    g.append(beacon);
  }
}

/**
 * Fills a volume with windows: horizontal strips up to the block, continuous ribbons on towers.
 *
 * Strips, never squares: two squares above a door make a face, and that is what makes a drawn
 * city childish. Whether a window is lit is drawn per window and floor, then read against the
 * hour: the same windows switch on every evening, floor by floor, as in a real street.
 */
function appendWindows(
  g: SVGGElement,
  lot: Lot,
  tier: number,
  volume: BuildingVolume,
  volumeIndex: number,
  sky: SkyState,
  tone: (color: string) => string,
  reducedMotion: boolean,
): void {
  const ribbon = tier >= TOWER_TIER;
  const floorH = tier === 0 ? 12 : ribbon ? 9 : 10 + Math.min(tier, 3) * 0.5;
  const winH = ribbon ? 3.2 : 3.5;
  const usable = volume.w - 3;
  const floors = Math.max(1, Math.floor((volume.bottom - volume.top - 6) / floorH));

  let cols: number;
  let winW: number;
  let gap: number;
  if (tier === 0) {
    cols = 1;
    winW = Math.max(5, Math.round(volume.w * 0.45));
    gap = 0;
  } else if (ribbon) {
    cols = Math.max(1, Math.round((usable - 6) / 22));
    gap = 3;
    winW = (usable - 6 - gap * (cols - 1)) / cols;
  } else {
    winW = 7 + Math.min(tier, 3);
    gap = 5;
    cols = Math.max(1, Math.floor((usable - 6 + gap) / (winW + gap)));
  }
  const left = volume.x + (usable - (cols * winW + (cols - 1) * gap)) / 2;
  const glass = tone(sky.glass);

  for (let floor = 0; floor < floors; floor++) {
    const y = volume.top + 5 + floor * floorH;
    if (y + winH > HORIZON - 3) {
      break;
    }
    const floorRoll = hashUnit(lot.id, tier, volumeIndex, floor);
    for (let col = 0; col < cols; col++) {
      const key = hashUnit(lot.id, tier, volumeIndex, floor, col);
      const roll = floorRoll * 0.55 + key * 0.45;
      const on = roll < sky.lit;
      const warm = key < 0.2 ? TOWN_PALETTE.warmCore : TOWN_PALETTE.warm;
      const rect = svgElement('rect', {
        x: (left + col * (winW + gap)).toFixed(1),
        y: y.toFixed(1),
        width: winW.toFixed(1),
        height: winH,
        fill: on ? warm : glass,
        opacity: on ? 0.92 : 0.85,
      });

      // Windows at the edge of the hour's share switch on and off now and then: the city lives.
      if (!reducedMotion && sky.lit > 0.1 && Math.abs(roll - sky.lit) < 0.04) {
        rect.setAttribute('fill', warm);
        rect.setAttribute('opacity', on ? '0.92' : '0.1');
        const blink = animate(
          on ? '0.92;0.1' : '0.1;0.92',
          `${(40 + key * 80).toFixed(0)}s`,
          `-${(key * 97).toFixed(0)}s`,
        );
        blink.setAttribute('calcMode', 'discrete');
        rect.append(blink);
      }
      g.append(rect);
    }
  }
}

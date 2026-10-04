import { svgElement } from '@core/svg/svg-element.utils';
import { hashUnit } from '@core/random/hash-unit.utils';
import { animate } from '@shared/rocket/rocket-drawing.utils';
import { BuildingShape, BuildingVolume, Lot, SkyState, WindowGrid } from './town-scene.model';
import {
  BLOCK_TIER,
  CABIN_TIER,
  CONCRETE_SHARE,
  FACADE_TINTS,
  HORIZON,
  HOUSE_TIER,
  ROOF_TILE,
  SMALL_BUILDING_TIER,
  TOP_TIER,
  TOWER_TIER,
  TOWN_PALETTE,
} from './town-scene.constants';
import { mixColor } from './town-sky-cycle.utils';

/**
 * Each tier is a building type, not a size, so the city changes character as it grows.
 */

/**
 * Draws a lot's building; the lot id seeds every draw so it looks the same across renders.
 */
export function drawBuilding(
  lot: Lot,
  shape: BuildingShape,
  sky: SkyState,
  reducedMotion: boolean,
): SVGGElement {
  // The back row sits in the haze.
  const tone = (color: string): string =>
    lot.row === 'back' ? mixColor(color, sky.haze, 0.24) : color;
  const wall = tone(facade(lot, shape.tier, sky));
  const roof = tone(sky.roof);
  const volumes = stackVolumes(shape);
  const g = svgElement('g');

  for (const volume of volumes) {
    appendVolume(g, volume, shape.tier, wall, roof);
  }
  appendRoof(g, lot, shape, roof, sky, reducedMotion);
  volumes.forEach((volume, index) =>
    appendWindows(g, lot, shape.tier, volume, index, sky, tone, reducedMotion),
  );
  // Lit sign on some blocks, the city's only colour besides windows.
  if (shape.tier === BLOCK_TIER && hashUnit(lot.id, 6) < 0.3) {
    appendSign(g, shape);
  }
  return g;
}

/**
 * Stacked volumes, bottom first: one setback on a tower, two on a skyscraper, none below.
 */
function stackVolumes({ tier, x, w, h }: BuildingShape): BuildingVolume[] {
  const top = HORIZON - h;
  if (tier === TOWER_TIER) {
    const cut = top + h * 0.22;
    const inset = Math.round(w * 0.14);
    return [
      { x, w, top: cut, bottom: HORIZON },
      { x: x + inset, w: w - inset * 2, top, bottom: cut },
    ];
  }
  if (tier === TOP_TIER) {
    const cutHigh = top + h * 0.12;
    const cutLow = top + h * 0.34;
    const inset = Math.round(w * 0.12);
    return [
      { x, w, top: cutLow, bottom: HORIZON },
      { x: x + inset, w: w - inset * 2, top: cutHigh, bottom: cutLow },
      { x: x + inset * 2.2, w: w - inset * 4.4, top, bottom: cutHigh },
    ];
  }
  return [{ x, w, top, bottom: HORIZON }];
}

/**
 * One volume: wall, shaded right strip, and a cornice from the small building up.
 */
function appendVolume(
  g: SVGGElement,
  volume: BuildingVolume,
  tier: number,
  wall: string,
  roof: string,
): void {
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
  if (tier >= SMALL_BUILDING_TIER) {
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

/**
 * Cyan sign across a block's facade.
 */
function appendSign(g: SVGGElement, { x, w, h }: BuildingShape): void {
  const top = HORIZON - h;
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

/**
 * Facade colour: concrete or paint, stronger on houses than towers, paler at night.
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
 * Adds the roof feature matching the tier.
 */
function appendRoof(
  g: SVGGElement,
  lot: Lot,
  shape: BuildingShape,
  roof: string,
  sky: SkyState,
  reducedMotion: boolean,
): void {
  if (shape.tier <= HOUSE_TIER) {
    appendPitchedRoof(g, lot, shape, roof, sky);
  } else if (shape.tier === SMALL_BUILDING_TIER) {
    appendWaterTank(g, lot, shape, roof);
  } else if (shape.tier === BLOCK_TIER) {
    appendRooftopPlant(g, lot, shape, roof);
  } else {
    appendTowerCrown(g, lot, shape, reducedMotion);
  }
}

/**
 * Pitched roof of a cabin or house, tiled on some lots, chimney on some houses.
 */
function appendPitchedRoof(
  g: SVGGElement,
  lot: Lot,
  { tier, x, w, h }: BuildingShape,
  roof: string,
  sky: SkyState,
): void {
  const top = HORIZON - h;
  const pitch = tier === CABIN_TIER ? 6 : 9;
  const tiled = hashUnit(lot.id, 10) < 0.55;
  g.append(
    svgElement('path', {
      d: `M${x - 2} ${top + 1} L${x + w / 2} ${top - pitch} L${x + w + 2} ${top + 1} Z`,
      fill: tiled ? mixColor(roof, ROOF_TILE, 0.3 + 0.3 * (1 - sky.lamps)) : roof,
    }),
  );
  const hasChimney = tier === HOUSE_TIER && hashUnit(lot.id, 7) < 0.55;
  if (hasChimney) {
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
}

/**
 * Water tank on two legs along a small building's roof.
 */
function appendWaterTank(g: SVGGElement, lot: Lot, { x, w, h }: BuildingShape, roof: string): void {
  const top = HORIZON - h;
  const tx = x + 4 + hashUnit(lot.id, 7) * (w - 14);
  g.append(
    svgElement('rect', { x: tx + 1, y: top - 4, width: 1, height: 4, fill: roof }),
    svgElement('rect', { x: tx + 5, y: top - 4, width: 1, height: 4, fill: roof }),
    svgElement('rect', { x: tx, y: top - 10, width: 7, height: 6, fill: roof }),
  );
}

/**
 * Rooftop plant of a block: a low box a third of the roof wide.
 */
function appendRooftopPlant(
  g: SVGGElement,
  lot: Lot,
  { x, w, h }: BuildingShape,
  roof: string,
): void {
  const top = HORIZON - h;
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
}

/**
 * Tower crown: light facade edge and mast, plus a red beacon on a skyscraper.
 */
function appendTowerCrown(
  g: SVGGElement,
  lot: Lot,
  { tier, x, w, h }: BuildingShape,
  reducedMotion: boolean,
): void {
  const top = HORIZON - h;
  const isSkyscraper = tier === TOP_TIER;
  const mast = isSkyscraper ? 24 : 12;
  g.append(
    svgElement('rect', {
      x: x + 3,
      y: top + h * 0.25,
      width: 1.5,
      height: h * 0.75,
      fill: TOWN_PALETTE.steelLit,
      opacity: 0.5,
    }),
    svgElement('rect', {
      x: x + w / 2 - 0.75,
      y: top - mast,
      width: 1.5,
      height: mast,
      fill: TOWN_PALETTE.mast,
    }),
  );
  if (!isSkyscraper) {
    return;
  }
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

/**
 * Fills a volume with windows: strips up to the block, ribbons on towers (never squares: they read
 * as faces). Stable per-window draws make the same windows light up every evening.
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
  const grid = windowGrid(tier, volume);
  const glass = tone(sky.glass);

  for (let floor = 0; floor < grid.floors; floor++) {
    const y = volume.top + 5 + floor * grid.floorH;
    // No window on the ground strip.
    if (y + grid.winH > HORIZON - 3) {
      break;
    }
    const floorRoll = hashUnit(lot.id, tier, volumeIndex, floor);
    for (let col = 0; col < grid.cols; col++) {
      const key = hashUnit(lot.id, tier, volumeIndex, floor, col);
      // Mostly the floor's draw, so lights come on floor by floor.
      const roll = floorRoll * 0.55 + key * 0.45;
      const on = roll < sky.lit;
      const warm = key < 0.2 ? TOWN_PALETTE.warmCore : TOWN_PALETTE.warm;
      const rect = svgElement('rect', {
        x: (grid.left + col * (grid.winW + grid.gap)).toFixed(1),
        y: y.toFixed(1),
        width: grid.winW.toFixed(1),
        height: grid.winH,
        fill: on ? warm : glass,
        opacity: on ? 0.92 : 0.85,
      });

      // Windows at the edge of the hour's lit share flicker now and then.
      const flickers = !reducedMotion && sky.lit > 0.1 && Math.abs(roll - sky.lit) < 0.04;
      if (flickers) {
        makeBlink(rect, on, warm, key);
      }
      g.append(rect);
    }
  }
}

/**
 * Window layout: one window on a cabin, ribbons on towers, strips otherwise.
 */
function windowGrid(tier: number, volume: BuildingVolume): WindowGrid {
  const usable = volume.w - 3;
  const floorH = floorHeight(tier);
  const { cols, winW, gap } = windowColumns(tier, volume.w, usable);
  return {
    floors: Math.max(1, Math.floor((volume.bottom - volume.top - 6) / floorH)),
    floorH,
    winH: tier >= TOWER_TIER ? 3.2 : 3.5,
    cols,
    winW,
    gap,
    left: volume.x + (usable - (cols * winW + (cols - 1) * gap)) / 2,
  };
}

/**
 * Floor height: tallest on a cabin, tightest on towers.
 */
function floorHeight(tier: number): number {
  if (tier === CABIN_TIER) {
    return 12;
  }
  if (tier >= TOWER_TIER) {
    return 9;
  }
  return 10 + Math.min(tier, BLOCK_TIER) * 0.5;
}

/**
 * Columns of windows across a floor of `usable` width.
 */
function windowColumns(
  tier: number,
  volumeW: number,
  usable: number,
): Pick<WindowGrid, 'cols' | 'winW' | 'gap'> {
  if (tier === CABIN_TIER) {
    return { cols: 1, winW: Math.max(5, Math.round(volumeW * 0.45)), gap: 0 };
  }
  // Ribbons about 22 units wide, stretched to fill the floor exactly.
  if (tier >= TOWER_TIER) {
    const cols = Math.max(1, Math.round((usable - 6) / 22));
    const gap = 3;
    return { cols, winW: (usable - 6 - gap * (cols - 1)) / cols, gap };
  }
  const winW = 7 + Math.min(tier, BLOCK_TIER);
  const gap = 5;
  return { cols: Math.max(1, Math.floor((usable - 6 + gap) / (winW + gap))), winW, gap };
}

/**
 * Makes a window switch slowly on and off, starting from its current state.
 */
function makeBlink(rect: SVGRectElement, on: boolean, warm: string, key: number): void {
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

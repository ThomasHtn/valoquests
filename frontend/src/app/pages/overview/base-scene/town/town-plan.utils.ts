import { createSeededRandom } from '@core/random/seeded-random.utils';
import { hashUnit } from '@core/random/hash-unit.utils';
import { BuildingShape, Lot, LotRow, LotSite } from './town-scene.model';
import {
  BLOCK_TIER,
  CABIN_TIER,
  FARTHEST_LOT_ORDER,
  FINISHED_CITY_GROWTH,
  FOUNDING_CAMP,
  GROWTH_CURVE,
  HOUSE_TIER,
  LAUNCH_PLOT,
  ROCKET_X,
  ROW_LAYOUT,
  SMALL_BUILDING_TIER,
  TIER_HEIGHTS,
  TIER_PACE,
  TOP_TIER,
  TOWER_TIER,
  TOWN_SEED,
  TOWN_WIDTH,
} from './town-scene.constants';

/**
 * Plan independent of population: tomorrow's city is today's plus a building, never a reshuffle.
 */

/**
 * Share of the finished city standing for a population, in [0, 1].
 */
export function growthOf(population: number, fullCampaignPopulation: number): number {
  const share = Math.max(0, population) / Math.max(1, fullCampaignPopulation);
  return Math.min(1, share ** GROWTH_CURVE);
}

/**
 * Lays out the lots (back row first, drawn behind) and schedules their buildings.
 * Constructions are spread evenly over growth so every daily visit shows something new.
 */
export function planCity(): readonly Lot[] {
  const sites = [...layoutRow('back'), ...layoutRow('front')];
  const keys = sites.map((site, id) => orderKeys(id, site));

  const events = keys
    .flatMap((lotKeys, id) => lotKeys.map((key, tier) => ({ id, tier, key })))
    .sort((a, b) => a.key - b.key);
  const thresholds = keys.map(() => [] as number[]);
  events.forEach((event, rank) => {
    thresholds[event.id][event.tier] =
      rank < FOUNDING_CAMP ? 0 : (rank / events.length) * FINISHED_CITY_GROWTH;
  });

  return sites.map((site, id) => ({
    ...site,
    id,
    thresholds: thresholds[id],
    scale: 0.86 + hashUnit(id, 4) * 0.28,
  }));
}

/**
 * City lots, planned once since the plan depends on no input.
 */
export const CITY_LOTS = planCity();

/**
 * Tier a lot has reached at a growth, -1 while it is still empty.
 */
export function tierAt(lot: Lot, growth: number): number {
  let tier = -1;
  lot.thresholds.forEach((threshold, index) => {
    if (growth >= threshold) {
      tier = index;
    }
  });
  return tier;
}

/**
 * Building of a lot at a tier; cabins and houses sit off-centre, larger ones fill the lot.
 */
export function buildingAt(lot: Lot, tier: number): BuildingShape {
  const h = Math.round(TIER_HEIGHTS[tier] * lot.scale);
  if (tier >= SMALL_BUILDING_TIER) {
    return { tier, x: lot.x, w: lot.w, h };
  }
  const baseWidth = tier === CABIN_TIER ? 14 : 20;
  const w = Math.min(lot.w, Math.round(baseWidth + hashUnit(lot.id, 11) * 8));
  const x = lot.x + Math.round((lot.w - w) * hashUnit(lot.id, 12));
  return { tier, x, w, h };
}

/**
 * Cuts one row into plots, jumping over the launch plot.
 */
function layoutRow(row: LotRow): LotSite[] {
  const random = createSeededRandom(TOWN_SEED + (row === 'back' ? 1 : 2));
  const layout = ROW_LAYOUT[row];
  const sites: LotSite[] = [];
  let x = layout.start;
  while (x < TOWN_WIDTH) {
    const w = Math.round(layout.minWidth + random() * (layout.maxWidth - layout.minWidth));
    if (x + w > LAUNCH_PLOT[0] && x < LAUNCH_PLOT[1]) {
      x = LAUNCH_PLOT[1];
      continue;
    }
    sites.push({ row, x, w });
    x += w + Math.round(layout.minGap + random() * (layout.maxGap - layout.minGap));
  }
  return sites;
}

/**
 * Order keys of a lot's constructions, one per tier, increasing so it is built before rebuilt.
 */
function orderKeys(id: number, { row, x, w }: LotSite): number[] {
  const distance = Math.abs(x + w / 2 - ROCKET_X) / ROCKET_X;
  const roll = hashUnit(id, 1);
  const finalTier = row === 'front' ? frontTier(distance, roll) : backTier(distance, roll);

  const reach = Math.max(0, (distance - 0.2) / 0.8) ** 1.15;
  const born = FARTHEST_LOT_ORDER * reach + (row === 'back' ? 0.06 : 0) + hashUnit(id, 2) * 0.05;
  const pace = TIER_PACE * (0.8 + hashUnit(id, 3) * 0.4);
  return Array.from({ length: finalTier + 1 }, (_, tier) => born + tier * pace);
}

/**
 * Final tier of a front-row lot: low near the pad so the rocket dominates, a block at most.
 */
function frontTier(distance: number, roll: number): number {
  if (distance < 0.32) {
    return roll < 0.5 ? HOUSE_TIER : SMALL_BUILDING_TIER;
  }
  return roll < 0.4 ? BLOCK_TIER : SMALL_BUILDING_TIER;
}

/**
 * Final tier of a back-row lot: towers everywhere, skyscrapers kept away from the pad.
 */
function backTier(distance: number, roll: number): number {
  if (distance < 0.28) {
    return roll < 0.45 ? TOWER_TIER : BLOCK_TIER;
  }
  return roll < (distance < 0.5 ? 0.4 : 0.6) ? TOP_TIER : TOWER_TIER;
}

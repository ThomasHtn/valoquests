import { createSeededRandom } from '@core/random/seeded-random.utils';
import { hashUnit } from '@core/random/hash-unit.utils';
import { BuildingShape, Lot, LotRow } from './town-scene.model';
import {
  COMPLETE_AT,
  FOUNDING_CAMP,
  GROWTH_CURVE,
  PLOT,
  ROW_LAYOUT,
  RX,
  SPREAD,
  TIER_HEIGHTS,
  TIER_PACE,
  TOP_TIER,
  TOWN_SEED,
  TOWN_WIDTH,
} from './town-scene.constants';

/**
 * The city as a plan: fixed lots, each built on at its own growth and rebuilt taller at the next.
 *
 * The plan never depends on the population, only its reading does: a lot keeps its place and its
 * draws for the whole campaign, so the city seen tomorrow is today's plus a building or a floor,
 * never a reshuffle. Births spread from the launch pad toward the edges, the base having been
 * founded where the ship is built.
 */

/**
 * Growth of the city, in [0, 1], for a population.
 *
 * @param population - Inhabitants of the base.
 * @param fullCampaignPopulation - Population a full campaign is expected to reach.
 * @returns The share of the finished city standing.
 */
export function growthOf(population: number, fullCampaignPopulation: number): number {
  const share = Math.max(0, population) / Math.max(1, fullCampaignPopulation);
  return Math.min(1, share ** GROWTH_CURVE);
}

/**
 * Lays out every lot of the city and schedules its buildings.
 *
 * Every construction of the campaign (a lot built on, a lot rebuilt a tier higher) is ordered by a
 * key, then spread evenly over the growth: the city changes by one building at a steady pace
 * rather than in bursts, which is what makes a daily visit show something new.
 *
 * @returns The lots, back row first so it is drawn behind the front one.
 */
export function planCity(): readonly Lot[] {
  const sites = [...layoutRow('back'), ...layoutRow('front')];
  const keys = sites.map((site, id) => orderKeys(id, site.row, site.x, site.w));

  const events = keys
    .flatMap((lotKeys, id) => lotKeys.map((key, tier) => ({ id, tier, key })))
    .sort((a, b) => a.key - b.key);
  const thresholds = keys.map(() => [] as number[]);
  events.forEach((event, rank) => {
    thresholds[event.id][event.tier] =
      rank < FOUNDING_CAMP ? 0 : (rank / events.length) * COMPLETE_AT;
  });

  return sites.map((site, id) => ({
    ...site,
    id,
    thresholds: thresholds[id],
    scale: 0.86 + hashUnit(id, 4) * 0.28,
  }));
}

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
 * The building a lot carries at a tier.
 *
 * Cabins and houses are narrower than their lot and sit off-centre within it; from the small
 * building up, the lot is built from edge to edge.
 */
export function buildingAt(lot: Lot, tier: number): BuildingShape {
  const h = Math.round(TIER_HEIGHTS[tier] * lot.scale);
  if (tier >= 2) {
    return { tier, x: lot.x, w: lot.w, h };
  }
  const base = tier === 0 ? 14 : 20;
  const w = Math.min(lot.w, Math.round(base + hashUnit(lot.id, 11) * 8));
  const x = lot.x + Math.round((lot.w - w) * hashUnit(lot.id, 12));
  return { tier, x, w, h };
}

/**
 * Cuts one row into plots, jumping over the launch plot.
 */
function layoutRow(row: LotRow): { row: LotRow; x: number; w: number }[] {
  const rnd = createSeededRandom(TOWN_SEED + (row === 'back' ? 1 : 2));
  const layout = ROW_LAYOUT[row];
  const sites: { row: LotRow; x: number; w: number }[] = [];
  let x = layout.start;
  while (x < TOWN_WIDTH) {
    const w = Math.round(layout.minW + rnd() * (layout.maxW - layout.minW));
    if (x + w > PLOT[0] && x < PLOT[1]) {
      x = PLOT[1];
      continue;
    }
    sites.push({ row, x, w });
    x += w + Math.round(layout.minGap + rnd() * (layout.maxGap - layout.minGap));
  }
  return sites;
}

/**
 * Order keys of a lot's constructions, one per tier up to its final one, increasing with the
 * tier so a lot is never rebuilt before it is built.
 */
function orderKeys(id: number, row: LotRow, x: number, w: number): number[] {
  const distance = Math.abs(x + w / 2 - RX) / RX;
  const roll = hashUnit(id, 1);
  const finalTier = row === 'front' ? frontTier(distance, roll) : backTier(distance, roll);

  const reach = Math.max(0, (distance - 0.2) / 0.8) ** 1.15;
  const born = SPREAD * reach + (row === 'back' ? 0.06 : 0) + hashUnit(id, 2) * 0.05;
  const pace = TIER_PACE * (0.8 + hashUnit(id, 3) * 0.4);
  return Array.from({ length: finalTier + 1 }, (_, tier) => born + tier * pace);
}

/**
 * Final tier of a front-row lot: low near the pad so the rocket dominates, a block at most.
 */
function frontTier(distance: number, roll: number): number {
  if (distance < 0.32) {
    return roll < 0.5 ? 1 : 2;
  }
  return roll < 0.4 ? 3 : 2;
}

/**
 * Final tier of a back-row lot: towers everywhere, skyscrapers kept off the pad's shoulders.
 */
function backTier(distance: number, roll: number): number {
  if (distance < 0.28) {
    return roll < 0.45 ? 4 : 3;
  }
  return roll < (distance < 0.5 ? 0.4 : 0.6) ? TOP_TIER : 4;
}

import { describe, expect, it } from 'vitest';

import { buildingAt, growthOf, planCity, tierAt } from './city-plan.utils';
import { FOUNDING_CAMP, PLOT, TIER_HEIGHTS } from './town-scene.constants';

const lots = planCity();
const standing = (growth: number): number[] => lots.map((lot) => tierAt(lot, growth));
const events = lots.flatMap((lot) => lot.thresholds);

describe('planCity', () => {
  it('draws the same plan on every call', () => {
    expect(planCity()).toEqual(lots);
  });

  it('keeps the launch plot free', () => {
    for (const lot of lots) {
      expect(lot.x + lot.w <= PLOT[0] || lot.x >= PLOT[1]).toBe(true);
    }
  });

  it('opens with the founding camp', () => {
    expect(standing(0).filter((tier) => tier >= 0)).toHaveLength(FOUNDING_CAMP);
  });

  it('ends with every lot at its final tier, towers included', () => {
    const final = standing(1);
    lots.forEach((lot, i) => expect(final[i]).toBe(lot.thresholds.length - 1));
    expect(final).toContain(TIER_HEIGHTS.length - 1);
  });

  it('never demolishes as the city grows', () => {
    let previous = standing(0);
    for (let growth = 0.01; growth <= 1; growth += 0.01) {
      const current = standing(growth);
      current.forEach((tier, i) => expect(tier).toBeGreaterThanOrEqual(previous[i]));
      previous = current;
    }
  });

  it('spreads its constructions evenly, enough for about one a day', () => {
    expect(events.length).toBeGreaterThan(150);
    for (let bin = 0; bin < 9; bin++) {
      const inBin = events.filter((e) => e >= bin / 10 && e < (bin + 1) / 10).length;
      expect(inBin).toBeGreaterThan(events.length / 14);
    }
  });
});

describe('growthOf', () => {
  it('grows faster than the population share early on, and caps at one', () => {
    expect(growthOf(3_000, 30_000)).toBeGreaterThan(0.1);
    expect(growthOf(60_000, 30_000)).toBe(1);
    expect(growthOf(-5, 30_000)).toBe(0);
  });
});

describe('buildingAt', () => {
  it('keeps every building inside its lot and taller at each tier', () => {
    for (const lot of lots) {
      let previous = 0;
      lot.thresholds.forEach((_, tier) => {
        const shape = buildingAt(lot, tier);
        expect(shape.x).toBeGreaterThanOrEqual(lot.x);
        expect(shape.x + shape.w).toBeLessThanOrEqual(lot.x + lot.w);
        expect(shape.h).toBeGreaterThan(previous);
        previous = shape.h;
      });
    }
  });
});

import { describe, expect, it } from 'vitest';

import { ColonyTierName } from '@core/colony/colony.model';
import { buildTownScene, hasWaterAt, reflectionsFor, townStageFor } from './town-scene';

const TIER_NAMES: readonly ColonyTierName[] = [
  'CAMP',
  'HAMLET',
  'VILLAGE',
  'BOROUGH',
  'TOWN',
  'CITY',
  'RESIDENTIAL_QUARTER',
  'GREAT_CITY',
  'METROPOLIS',
  'MEGALOPOLIS',
  'CAPITAL',
  'CITADEL',
];

describe('townStageFor', () => {
  it('gives every step of the ladder its own stage, lowest first', () => {
    const stages = TIER_NAMES.map(townStageFor);

    expect(stages).toEqual([...stages].sort((a, b) => a - b));
    expect(new Set(stages).size).toBe(TIER_NAMES.length);
  });
});

describe('buildTownScene', () => {
  it('builds something at every step', () => {
    for (const tier of TIER_NAMES) {
      const scene = buildTownScene(townStageFor(tier));

      expect(scene.buildings.length, tier).toBeGreaterThan(0);
      expect(scene.terraces.length, tier).toBeGreaterThan(0);
      expect(scene.foreground.length, tier).toBeGreaterThan(0);
    }
  });

  it('retires the first houses once the waterfront is rebuilt over them', () => {
    const camp = buildTownScene(townStageFor('CAMP')).buildings.length;
    const borough = buildTownScene(townStageFor('BOROUGH')).buildings.length;
    const town = buildTownScene(townStageFor('TOWN')).buildings.length;

    // the camp's four structures stand through the borough; the town drops all four and puts up one
    // mid-rise in their place
    expect(camp).toBe(4);
    expect(borough).toBeGreaterThan(camp);
    expect(town).toBe(borough - 4 + 1);
  });

  it('keeps the frontage centred on what it actually contains', () => {
    for (const tier of TIER_NAMES) {
      const scene = buildTownScene(townStageFor(tier));
      const left = Math.min(...scene.buildings.map((building) => building.x));
      const right = Math.max(...scene.buildings.map((building) => building.x + building.w));

      expect((left + right) / 2 + scene.frontageOffset, tier).toBeCloseTo(360, 5);
    }
  });

  it('digs the water out only once the colony has grown past its camp', () => {
    expect(hasWaterAt(townStageFor('CAMP'))).toBe(false);
    expect(hasWaterAt(townStageFor('HAMLET'))).toBe(true);
    expect(hasWaterAt(townStageFor('CITADEL'))).toBe(true);
  });

  it('reflects one smear per building, and none without water', () => {
    const city = buildTownScene(townStageFor('CITY'));

    expect(reflectionsFor(city.buildings)).toHaveLength(city.buildings.length);
    expect(reflectionsFor([])).toHaveLength(0);
  });
});

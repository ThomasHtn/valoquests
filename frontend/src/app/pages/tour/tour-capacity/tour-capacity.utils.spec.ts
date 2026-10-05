import { describe, expect, it } from 'vitest';

import { TOUR_SAMPLE_CAPACITY } from '../tour-samples.constants';
import { buildCapacityTiles } from './tour-capacity.utils';

describe('buildCapacityTiles', () => {
  const [carry, shelter] = buildCapacityTiles(TOUR_SAMPLE_CAPACITY);

  it('puts the components dial before the food dial', () => {
    expect([carry.kind, shelter.kind]).toEqual(['carry', 'shelter']);
  });

  it('reads each dial from its own stock and rescue cost', () => {
    expect(carry.gauge).toBe(TOUR_SAMPLE_CAPACITY.carry);
    expect(carry.rescueCost).toBe(TOUR_SAMPLE_CAPACITY.componentsPerRescue);
    expect(shelter.gauge).toBe(TOUR_SAMPLE_CAPACITY.shelter);
    expect(shelter.rescueCost).toBe(TOUR_SAMPLE_CAPACITY.foodPerRescue);
  });
});

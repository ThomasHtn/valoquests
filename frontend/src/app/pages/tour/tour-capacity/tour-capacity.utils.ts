import { LucideBuilding2 } from '@lucide/angular';

import { CONCEPT_ICONS } from '@core/concepts/concept.constants';
import {
  CARRY_MODES,
  SHELTER_MODES,
} from '@pages/overview/extraction-gauges/extraction-gauges.constants';
import { Capacity } from '@pages/overview/extraction-gauges/extraction-gauges.model';

import { TourCapacityTile } from './tour-capacity.model';

/**
 * The components dial, then the food dial, in the overview's order.
 */
export function buildCapacityTiles(capacity: Capacity): readonly TourCapacityTile[] {
  return [
    {
      kind: 'carry',
      nameKey: 'overview.capacity.carry',
      gauge: capacity.carry,
      gaugeIcon: CONCEPT_ICONS.rocket,
      resourceIcon: CONCEPT_ICONS.components,
      resourceKey: 'common.resource.components',
      modes: CARRY_MODES,
      rescueCost: capacity.componentsPerRescue,
      rescueCostKey: 'overview.capacity.carryRate',
    },
    {
      kind: 'shelter',
      nameKey: 'overview.capacity.shelter',
      gauge: capacity.shelter,
      gaugeIcon: LucideBuilding2,
      resourceIcon: CONCEPT_ICONS.food,
      resourceKey: 'common.resource.food',
      modes: SHELTER_MODES,
      rescueCost: capacity.foodPerRescue,
      rescueCostKey: 'overview.capacity.shelterRate',
    },
  ];
}

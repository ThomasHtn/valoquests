import { SquadColumn } from './squad-sheet.model';

/**
 * Header columns in row order, after the rank's hex counter.
 */
export const SQUAD_COLUMNS: readonly SquadColumn[] = [
  { label: 'overview.squad.operator', tooltip: 'overview.squad.operatorTooltip' },
  { label: 'overview.squad.damage', tooltip: 'overview.squad.damageTooltip' },
  { label: 'common.resource.components', tooltip: 'overview.squad.componentsTooltip' },
  { label: 'common.resource.food', tooltip: 'overview.squad.foodTooltip' },
  { label: 'overview.squad.streak', tooltip: 'overview.squad.streakTooltip' },
];

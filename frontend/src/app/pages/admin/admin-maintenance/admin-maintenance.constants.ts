import { ResetDataGroup } from './admin-maintenance.model';

/**
 * What the campaign reset clears then keeps, spelled out to show what survives.
 */
export const RESET_DATA_GROUPS: readonly ResetDataGroup[] = [
  {
    tone: 'cleared',
    labelKey: 'admin.maintenance.reset.clearedLabel',
    itemKeys: [
      'admin.maintenance.reset.cleared.matches',
      'admin.maintenance.reset.cleared.challenges',
      'admin.maintenance.reset.cleared.rankings',
      'admin.maintenance.reset.cleared.campaigns',
      'admin.maintenance.reset.cleared.synchronizations',
    ],
  },
  {
    tone: 'kept',
    labelKey: 'admin.maintenance.reset.keptLabel',
    itemKeys: [
      'admin.maintenance.reset.kept.players',
      'admin.maintenance.reset.kept.challengeCatalogue',
      'admin.maintenance.reset.kept.guardianCatalogue',
    ],
  },
];

import { HistoryColumn } from './campaign-history.model';

/**
 * Table columns headed by a concept icon, in display order.
 */
export const ICON_COLUMNS: readonly HistoryColumn[] = [
  { concept: 'base', labelKey: 'common.resource.base' },
  { concept: 'guardian', labelKey: 'campaign.history.colGuardians' },
  { concept: 'wounded', labelKey: 'common.resource.wounded' },
];

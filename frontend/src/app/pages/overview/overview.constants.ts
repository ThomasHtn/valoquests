import { OverviewTabKey } from './overview.model';

/**
 * Population a campaign run to its end is expected to reach at the normal tier: the scale the
 * city grows on, so a base that went the distance fills its whole skyline.
 */
export const FULL_CAMPAIGN_POPULATION = 30_000;

/**
 * Browser-side memory of the last report seen, so the dialog opens once per settled week. Storage
 * can be unavailable (private window, blocked site data): then the report simply opens again.
 */
export const SEEN_REPORT_KEY = 'valoquests.missionReport.seen';

/**
 * The overview's tabs, in bar order.
 */
export const OVERVIEW_TABS: readonly OverviewTabKey[] = [
  'challenges',
  'contributions',
  'matches',
  'campaign',
];

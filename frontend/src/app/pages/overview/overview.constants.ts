import { OverviewTabKey, OverviewTabLink, OverviewTabTone } from './overview.model';

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
 * The overview's tabs, in bar order, with the colour of the figure each one summarises and the
 * page that expands its panel.
 */
export const OVERVIEW_TABS: readonly {
  key: OverviewTabKey;
  tone: OverviewTabTone;
  link: OverviewTabLink | null;
}[] = [
  {
    key: 'challenges',
    tone: 'cyan',
    link: { route: '/challenges', labelKey: 'overview.orders.link' },
  },
  {
    key: 'contributions',
    tone: 'violet',
    link: { route: '/leaderboard', labelKey: 'overview.squad.link' },
  },
  { key: 'matches', tone: 'neutral', link: null },
  { key: 'campaign', tone: 'amber', link: null },
];

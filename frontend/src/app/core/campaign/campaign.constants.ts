import { WeeklyTitle } from './titles/campaign-title.model';
import { CampaignDifficulty, CampaignStartWeek } from './campaign.model';

/**
 * Difficulties, easiest first.
 */
export const CAMPAIGN_DIFFICULTIES: readonly CampaignDifficulty[] = ['AMATEUR', 'PRO'];

/**
 * Start weeks, in backoffice order.
 */
export const CAMPAIGN_START_WEEKS: readonly CampaignStartWeek[] = ['CURRENT_WEEK', 'NEXT_WEEK'];

/**
 * Weekly titles, in the backend's award order.
 */
export const WEEKLY_TITLES: readonly WeeklyTitle[] = [
  'REGULAR',
  'SCOUT',
  'QUARTERMASTER',
  'MECHANIC',
];

/**
 * Weeks in a campaign; mirrors the backend `CampaignSchedule.WEEK_COUNT`.
 */
export const CAMPAIGN_WEEK_COUNT = 10;

import { Campaign } from '@core/campaign/campaign.model';
import { addDays, daysBetween } from '@core/date/date.utils';
import { formatDateRange, formatDayMonth } from '@core/date/date-format.utils';

import { CAMPAIGN_DAYS } from './admin-campaigns.constants';
import { LiveCampaign } from './admin-campaigns.model';

/**
 * Formatted span of a campaign, its last Sunday included.
 */
export function formatCampaignRange(firstWeekStart: string, lastWeekStart: string): string {
  return formatDateRange(firstWeekStart, addDays(lastWeekStart, 6));
}

/**
 * Live block of an opened or running campaign, `null` between campaigns.
 */
export function buildLiveCampaign(campaign: Campaign | null | undefined): LiveCampaign | null {
  if (
    !campaign ||
    campaign.id === null ||
    campaign.status === null ||
    campaign.status === 'CLOSED' ||
    campaign.firstWeekStart === null ||
    campaign.lastWeekStart === null
  ) {
    return null;
  }

  // Clamped: day zero before the first Monday, never past the last day.
  const dayIndex = Math.min(
    CAMPAIGN_DAYS,
    Math.max(0, daysBetween(campaign.firstWeekStart, campaign.today) + 1),
  );

  return {
    id: campaign.id,
    number: campaign.number ?? 0,
    status: campaign.status,
    difficulty: campaign.difficulty ?? 'AMATEUR',
    reference: campaign.reference ?? 0,
    rosterSize: campaign.rosterSize ?? 0,
    range: formatCampaignRange(campaign.firstWeekStart, campaign.lastWeekStart),
    startsOn: formatDayMonth(campaign.firstWeekStart),
    weekIndex: campaign.currentWeekIndex ?? 0,
    dayIndex,
    daysLeft: CAMPAIGN_DAYS - dayIndex,
  };
}

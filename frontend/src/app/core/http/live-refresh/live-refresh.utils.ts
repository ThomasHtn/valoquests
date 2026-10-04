import { toCampaignDayKey } from '@core/campaign/calendar/campaign-calendar.utils';
import { DAY_TURN_GRACE_MS } from './live-refresh.constants';

/**
 * Comparable stamp of what changes backend data: the last synchronization and the campaign day.
 */
export function liveRefreshStamp(lastCompletedAt: string | null, now: Date): string {
  const day = toCampaignDayKey(new Date(now.getTime() - DAY_TURN_GRACE_MS).toISOString());
  return `${lastCompletedAt ?? ''}|${day}`;
}

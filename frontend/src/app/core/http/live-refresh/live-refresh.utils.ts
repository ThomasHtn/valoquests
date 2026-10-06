import { toCampaignDayKey } from '@core/campaign/calendar/campaign-calendar.utils';

import { DAY_TURN_GRACE_MS } from './live-refresh.constants';

/**
 * Comparable stamp of what changes backend data: the last import and the campaign day.
 */
export function liveRefreshStamp(lastImportedAt: string | null, now: Date): string {
  const day = toCampaignDayKey(new Date(now.getTime() - DAY_TURN_GRACE_MS).toISOString());
  return `${lastImportedAt ?? ''}|${day}`;
}

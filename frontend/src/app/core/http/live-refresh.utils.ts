import { toCampaignDayKey } from '@core/date/campaign-time-zone.utils';

/**
 * How long after midnight in the campaign time zone the day is considered to have turned.
 *
 * The backend closes a day at 00:10 (challenge drawn, meal written down by the nightly tick), so a
 * reload fired at 00:00 sharp would still read yesterday's state. Fifteen minutes leaves the tick
 * room to finish.
 */
export const DAY_TURN_GRACE_MS = 15 * 60_000;

/**
 * Summarizes everything that makes the backend's public data change, as one comparable string.
 *
 * Two things move the data: a synchronization finishing, which only happens once challenges and
 * campaign are rebuilt, and the day turning, which the nightly tick acts on with no synchronization
 * involved. A stamp that differs from the previous one means the screens are stale.
 *
 * @param lastCompletedAt - End of the last successful synchronization, or `null` when none did.
 * @param now - The current instant.
 * @returns The stamp.
 */
export function liveRefreshStamp(lastCompletedAt: string | null, now: Date): string {
  const day = toCampaignDayKey(new Date(now.getTime() - DAY_TURN_GRACE_MS).toISOString());
  return `${lastCompletedAt ?? ''}|${day}`;
}

import { CAMPAIGN_TIME_ZONE } from './date-time.constants';

/**
 * Formatter reading an instant's wall-clock fields in the campaign time zone.
 */
const CAMPAIGN_WALL_CLOCK = new Intl.DateTimeFormat('en-US', {
  timeZone: CAMPAIGN_TIME_ZONE,
  hourCycle: 'h23',
  year: 'numeric',
  month: 'numeric',
  day: 'numeric',
  hour: 'numeric',
  minute: 'numeric',
  second: 'numeric',
});

/**
 * Wall-clock fields of an instant, as read in the campaign time zone.
 */
interface WallClock {
  readonly year: number;
  readonly month: number;
  readonly day: number;
  readonly hour: number;
  readonly minute: number;
  readonly second: number;
}

/**
 * Reads the wall-clock fields of an instant in the campaign time zone.
 *
 * @param epochMs - The instant, in epoch milliseconds.
 * @returns Its wall-clock fields.
 */
function campaignWallClock(epochMs: number): WallClock {
  const fields: Record<string, number> = {};
  for (const part of CAMPAIGN_WALL_CLOCK.formatToParts(epochMs)) {
    if (part.type !== 'literal') {
      fields[part.type] = Number(part.value);
    }
  }
  return fields as unknown as WallClock;
}

/**
 * Offset of the campaign time zone from UTC at a given instant.
 *
 * @param epochMs - The instant, in epoch milliseconds.
 * @returns The offset in milliseconds, positive east of UTC.
 */
function campaignOffsetMs(epochMs: number): number {
  const wall = campaignWallClock(epochMs);
  const wallAsUtc = Date.UTC(
    wall.year,
    wall.month - 1,
    wall.day,
    wall.hour,
    wall.minute,
    wall.second,
  );
  return wallAsUtc - Math.floor(epochMs / 1000) * 1000;
}

/**
 * Resolves the instant a campaign day starts, i.e. 00:00 in the campaign time zone.
 *
 * @param isoDate - The day, as `YYYY-MM-DD`.
 * @param plusDays - Days to add to it, negative to go backward.
 * @returns The day's first instant, whatever the reader's own time zone.
 */
export function campaignMidnight(isoDate: string, plusDays = 0): Date {
  const [year, month, day] = isoDate.split('-').map(Number);
  const wallAsUtc = Date.UTC(year, month - 1, day + plusDays);
  // Second pass settles the offset when a daylight-saving change sits between the guess and midnight.
  const guess = wallAsUtc - campaignOffsetMs(wallAsUtc);
  return new Date(wallAsUtc - campaignOffsetMs(guess));
}

/**
 * Resolves the campaign day an ISO-8601 instant falls on.
 *
 * Read in the campaign time zone rather than the reader's, so a match lands on the same day the
 * backend credits it to.
 *
 * @param instant - The instant to resolve, as an ISO-8601 instant.
 * @returns The campaign day, as `YYYY-MM-DD`, usable as a grouping key.
 */
export function toCampaignDayKey(instant: string): string {
  const { year, month, day } = campaignWallClock(new Date(instant).getTime());
  return `${year}-${`${month}`.padStart(2, '0')}-${`${day}`.padStart(2, '0')}`;
}

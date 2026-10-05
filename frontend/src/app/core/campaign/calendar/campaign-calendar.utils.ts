import {
  MILLISECONDS_PER_DAY,
  MILLISECONDS_PER_HOUR,
  MILLISECONDS_PER_MINUTE,
} from '@core/date/date.constants';
import { WEEK_DAYS } from '@core/date/date.constants';
import { RemainingTime } from '@core/date/date.model';
import { daysBetween, parseIsoDate } from '@core/date/date.utils';
import { CAMPAIGN_WALL_CLOCK } from './campaign-calendar.constants';
import { WallClock } from './campaign-calendar.model';

/**
 * Wall-clock fields of an epoch-millisecond instant in the campaign time zone.
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
 * Campaign time zone offset from UTC at an instant, in milliseconds, positive east.
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
 * 00:00 in the campaign time zone of `isoDate` (`YYYY-MM-DD`) shifted by `plusDays`.
 */
export function campaignMidnight(isoDate: string, plusDays = 0): Date {
  const { year, month, day } = parseIsoDate(isoDate);
  const wallAsUtc = Date.UTC(year, month - 1, day + plusDays);
  // Second pass handles a daylight-saving change between the guess and midnight.
  const guess = wallAsUtc - campaignOffsetMs(wallAsUtc);
  return new Date(wallAsUtc - campaignOffsetMs(guess));
}

/**
 * Campaign day (`YYYY-MM-DD`) of an ISO-8601 instant, read in the backend's time zone.
 */
export function toCampaignDayKey(instant: string): string {
  const { year, month, day } = campaignWallClock(new Date(instant).getTime());
  return `${year}-${`${month}`.padStart(2, '0')}-${`${day}`.padStart(2, '0')}`;
}

/**
 * Time left until the weekly rollover (the day after `weekEnd`), clamped to zero.
 */
export function remainingWeekTime(weekEnd: string, now: Date): RemainingTime {
  // The backend rolls the week over on Monday 00:00 in the campaign zone, not the reader's.
  const deadline = campaignMidnight(weekEnd, 1).getTime();
  const remaining = Math.max(0, deadline - now.getTime());

  return {
    days: Math.floor(remaining / MILLISECONDS_PER_DAY),
    hours: Math.floor((remaining % MILLISECONDS_PER_DAY) / MILLISECONDS_PER_HOUR),
    minutes: Math.floor((remaining % MILLISECONDS_PER_HOUR) / MILLISECONDS_PER_MINUTE),
  };
}

/**
 * Zero-based position of `today` in the week starting `weekStart`, clamped to its seven days.
 */
export function weekDayIndex(weekStart: string, today: string): number {
  return Math.min(WEEK_DAYS - 1, Math.max(0, daysBetween(weekStart, today)));
}

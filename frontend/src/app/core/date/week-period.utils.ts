const MILLISECONDS_PER_MINUTE = 60_000;
const MILLISECONDS_PER_HOUR = 60 * MILLISECONDS_PER_MINUTE;
const MILLISECONDS_PER_DAY = 24 * MILLISECONDS_PER_HOUR;

/**
 * Time remaining until the end of a weekly period, split into whole units.
 */
export interface RemainingTime {
  readonly days: number;
  readonly hours: number;
  readonly minutes: number;
}

/**
 * Parses an ISO-8601 date (`YYYY-MM-DD`) into a UTC-midnight `Date`.
 *
 * Parsing manually (rather than via `new Date(isoDate)`) keeps the result anchored to UTC
 * regardless of the browser's local timezone, since `weekStart`/`weekEnd` are calendar dates
 * without a time component.
 *
 * @param isoDate - The ISO-8601 date to parse.
 * @returns The corresponding UTC-midnight date.
 */
function parseIsoDate(isoDate: string): Date {
  const [year, month, day] = isoDate.split('-').map(Number);
  return new Date(Date.UTC(year, month - 1, day));
}

/**
 * Computes the ISO-8601 week number of `isoDate`.
 *
 * @param isoDate - The date whose week number should be resolved, as `YYYY-MM-DD`.
 * @returns The ISO-8601 week number (1-53).
 */
export function isoWeekNumber(isoDate: string): number {
  const date = parseIsoDate(isoDate);
  const dayNumber = (date.getUTCDay() + 6) % 7;
  date.setUTCDate(date.getUTCDate() - dayNumber + 3);

  const firstThursday = new Date(Date.UTC(date.getUTCFullYear(), 0, 4));
  const firstThursdayDayNumber = (firstThursday.getUTCDay() + 6) % 7;
  firstThursday.setUTCDate(firstThursday.getUTCDate() - firstThursdayDayNumber + 3);

  return 1 + Math.round((date.getTime() - firstThursday.getTime()) / MILLISECONDS_PER_DAY / 7);
}

/**
 * Formats an ISO-8601 date as `DD/MM`.
 *
 * @param isoDate - The date to format, as `YYYY-MM-DD`.
 * @returns The date formatted as `DD/MM`.
 */
export function formatDayMonth(isoDate: string): string {
  const [, month, day] = isoDate.split('-');
  return `${day}/${month}`;
}

/**
 * Formats a week's start and end dates as `DD/MM - DD/MM`.
 *
 * @param weekStart - The week's start date, as `YYYY-MM-DD`.
 * @param weekEnd - The week's end date, as `YYYY-MM-DD`.
 * @returns The formatted date range.
 */
export function formatDateRange(weekStart: string, weekEnd: string): string {
  return `${formatDayMonth(weekStart)} - ${formatDayMonth(weekEnd)}`;
}

/**
 * Shifts an ISO-8601 date by a whole number of days.
 *
 * @param isoDate - The date to shift, as `YYYY-MM-DD`.
 * @param days - Number of days to add; negative values move backwards.
 * @returns The shifted date, as `YYYY-MM-DD`.
 */
export function addDays(isoDate: string, days: number): string {
  const shifted = new Date(parseIsoDate(isoDate).getTime() + days * MILLISECONDS_PER_DAY);
  return shifted.toISOString().slice(0, 10);
}

/**
 * Resolves the day the next weekly period starts on, i.e. the day after `weekEnd`.
 *
 * @param weekEnd - The active week's end date, as `YYYY-MM-DD`.
 * @returns The next week's start date, as `YYYY-MM-DD`.
 */
export function nextWeekStart(weekEnd: string): string {
  return addDays(weekEnd, 1);
}

/**
 * Computes the time remaining until the day after `weekEnd`, when the weekly rollover occurs.
 *
 * @param weekEnd - The active week's end date, as `YYYY-MM-DD`.
 * @param now - The current date, used to compute the remaining duration.
 * @returns The remaining time, clamped to zero once the deadline has passed.
 */
export function remainingWeekTime(weekEnd: string, now: Date): RemainingTime {
  // Local midnight, not UTC midnight: the backend closes a week on the Monday 00:00 of the group's
  // own timezone (`WeekCalendar`, `Europe/Paris`). Anchoring the deadline on `Date.UTC` made the
  // countdown run one to two hours past the rollover that had already happened, depending on
  // daylight saving. Read locally like every other date this application displays.
  const [year, month, day] = weekEnd.split('-').map(Number);
  const deadline = new Date(year, month - 1, day + 1).getTime();
  const remaining = Math.max(0, deadline - now.getTime());

  return {
    days: Math.floor(remaining / MILLISECONDS_PER_DAY),
    hours: Math.floor((remaining % MILLISECONDS_PER_DAY) / MILLISECONDS_PER_HOUR),
    minutes: Math.floor((remaining % MILLISECONDS_PER_HOUR) / MILLISECONDS_PER_MINUTE),
  };
}

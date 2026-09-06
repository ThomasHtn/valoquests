/**
 * Pads a number to two digits, as calendar and clock fields are always written.
 *
 * @param value - The value to pad.
 * @returns The value as a two-digit string.
 */
function padToTwoDigits(value: number): string {
  return `${value}`.padStart(2, '0');
}

/**
 * Milliseconds in a day.
 */
const MILLISECONDS_PER_DAY = 86_400_000;

/**
 * Parses an ISO date (`YYYY-MM-DD`) as local midnight, optionally offset by a number of days.
 *
 * @param isoDate - The day to parse, as `YYYY-MM-DD`.
 * @param plusDays - Days to add to the parsed date, negative to go backward.
 * @returns The resulting local midnight.
 */
export function localMidnight(isoDate: string, plusDays = 0): Date {
  const [year, month, day] = isoDate.split('-').map(Number);
  return new Date(year, month - 1, day + plusDays);
}

/**
 * Whole days from one ISO date to another, negative when the second comes first.
 *
 * Rounded rather than floored: a daylight-saving change between the two shifts the raw
 * millisecond difference by an hour either way, well short of the half-day this would need to tip
 * the rounding to the wrong calendar day.
 *
 * @param from - The starting day, as `YYYY-MM-DD`.
 * @param to - The ending day, as `YYYY-MM-DD`.
 * @returns The signed day count between the two.
 */
export function daysBetween(from: string, to: string): number {
  return Math.round(
    (localMidnight(to).getTime() - localMidnight(from).getTime()) / MILLISECONDS_PER_DAY,
  );
}

/**
 * Resolves the calendar day an ISO-8601 instant falls on, in the reader's timezone.
 *
 * Deliberately local rather than UTC: a match played at 00:30 belongs to the evening the player
 * remembers, not to the following UTC day.
 *
 * @param instant - The instant to resolve, as an ISO-8601 instant.
 * @returns The calendar day, as `YYYY-MM-DD`, usable as a grouping key.
 */
export function toLocalDayKey(instant: string): string {
  const date = new Date(instant);
  return `${date.getFullYear()}-${padToTwoDigits(date.getMonth() + 1)}-${padToTwoDigits(date.getDate())}`;
}

/**
 * BCP-47 locale backing {@link formatLocalDayMonth}'s month name, keyed by the app's own
 * `Language` codes (kept as a literal union here rather than importing `Language` from
 * `core/i18n`, so this module stays free of any dependency on the i18n module).
 */
const MONTH_NAME_LOCALES: Record<'fr' | 'en', string> = { fr: 'fr-FR', en: 'en-US' };

/**
 * Formats an ISO-8601 instant as a day and month in the reader's timezone, in the order
 * `language` writes dates (`"7 août"`, `"August 7"`).
 *
 * @param instant - The instant to format, as an ISO-8601 instant.
 * @param language - The app language whose month names and date order to use.
 * @param month - `'short'` abbreviates the month (`"7 août"` stays, `"7 sept."`, `"Sep 7"`) for a
 *   label that must fit a narrow column instead of truncating to an ellipsis.
 * @returns The formatted date.
 */
export function formatLocalDayMonth(
  instant: string,
  language: 'fr' | 'en',
  month: 'long' | 'short' = 'long',
): string {
  return new Intl.DateTimeFormat(MONTH_NAME_LOCALES[language], { day: 'numeric', month }).format(
    new Date(instant),
  );
}

/**
 * Formats a calendar day as `"<Weekday> DD/MM"` (e.g. `"Mardi 01/09"`), in `language`.
 *
 * Takes a calendar day rather than an instant, and reads it at noon UTC: the day the backend names is
 * already resolved against the rollover timezone, so re-projecting it through the reader's clock
 * would let a browser a few hours off print the wrong weekday for the very day it is showing.
 *
 * @param isoDate - The day to format, as `YYYY-MM-DD`.
 * @param language - The app language whose weekday names to use.
 * @returns The formatted day.
 */
export function formatWeekdayDayMonth(isoDate: string, language: 'fr' | 'en'): string {
  const date = new Date(`${isoDate}T12:00:00Z`);
  const weekday = new Intl.DateTimeFormat(MONTH_NAME_LOCALES[language], {
    weekday: 'long',
    timeZone: 'UTC',
  }).format(date);
  const [, month, day] = isoDate.split('-');

  return `${weekday.charAt(0).toUpperCase()}${weekday.slice(1)} ${day}/${month}`;
}

/**
 * Formats the time of day of an ISO-8601 instant as `HH:MM` in the reader's timezone.
 *
 * @param instant - The instant to format, as an ISO-8601 instant.
 * @returns The formatted time of day.
 */
export function formatLocalTime(instant: string): string {
  const date = new Date(instant);
  return `${padToTwoDigits(date.getHours())}:${padToTwoDigits(date.getMinutes())}`;
}

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
 * Formats an ISO-8601 instant as `"<Month> <day>"` (e.g. `"Août 7"`) in the reader's timezone,
 * with the month name spelled out in `language`.
 *
 * @param instant - The instant to format, as an ISO-8601 instant.
 * @param language - The app language whose month names to use.
 * @returns The formatted date.
 */
export function formatLocalDayMonth(instant: string, language: 'fr' | 'en'): string {
  const date = new Date(instant);
  const month = new Intl.DateTimeFormat(MONTH_NAME_LOCALES[language], { month: 'long' }).format(
    date,
  );
  return `${month.charAt(0).toUpperCase()}${month.slice(1)} ${date.getDate()}`;
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

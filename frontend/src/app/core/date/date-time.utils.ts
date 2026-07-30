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
 * Formats an ISO-8601 instant as `DD/MM/YYYY` in the reader's timezone.
 *
 * Formatted by hand rather than through `Intl`, as the rest of the application does: no locale is
 * registered, and a single numeric format keeps dates the same width everywhere they are listed.
 *
 * @param instant - The instant to format, as an ISO-8601 instant.
 * @returns The formatted date.
 */
export function formatLocalDate(instant: string): string {
  const date = new Date(instant);
  return `${padToTwoDigits(date.getDate())}/${padToTwoDigits(date.getMonth() + 1)}/${date.getFullYear()}`;
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

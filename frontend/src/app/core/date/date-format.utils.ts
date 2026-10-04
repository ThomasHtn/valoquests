import { resolveLocale } from '@core/i18n/format/locale.utils';
import { Language } from '@core/i18n/translation.model';
import {
  CAMPAIGN_CLOCK,
  CAMPAIGN_TIME_ZONE,
} from '@core/campaign/calendar/campaign-calendar.constants';

/**
 * How long ago an ISO instant was (`3 hr. ago`, "now" under a minute); `now` in milliseconds.
 */
export function formatElapsed(instant: string, now: number, language: Language): string {
  const format = new Intl.RelativeTimeFormat(resolveLocale(language), {
    numeric: 'auto',
    style: 'short',
  });
  const minutes = Math.floor(Math.max(0, now - Date.parse(instant)) / 60_000);
  if (minutes < 1) {
    return format.format(0, 'second');
  }
  if (minutes < 60) {
    return format.format(-minutes, 'minute');
  }
  const hours = Math.floor(minutes / 60);
  return hours < 24 ? format.format(-hours, 'hour') : format.format(-Math.floor(hours / 24), 'day');
}

/**
 * Formats a `YYYY-MM-DD` date as `DD/MM`.
 */
export function formatDayMonth(isoDate: string): string {
  const [, month, day] = isoDate.split('-');
  return `${day}/${month}`;
}

/**
 * Formats two `YYYY-MM-DD` dates as `DD/MM - DD/MM`.
 */
export function formatDateRange(weekStart: string, weekEnd: string): string {
  return `${formatDayMonth(weekStart)} - ${formatDayMonth(weekEnd)}`;
}

/**
 * Day and month of an instant (`7 août`, `August 7`), in the campaign time zone by default.
 */
export function formatLocalDayMonth(
  instant: string,
  language: Language,
  month: 'long' | 'short' = 'long',
  timeZone: string = CAMPAIGN_TIME_ZONE,
): string {
  return new Intl.DateTimeFormat(resolveLocale(language), {
    day: 'numeric',
    month,
    timeZone,
  }).format(new Date(instant));
}

/**
 * `HH:MM` of an instant in the campaign time zone, the one match days are grouped in.
 */
export function formatLocalTime(instant: string): string {
  return CAMPAIGN_CLOCK.format(new Date(instant));
}

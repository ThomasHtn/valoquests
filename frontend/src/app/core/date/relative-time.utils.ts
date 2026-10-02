import { resolveLocale } from '@core/i18n/locale.utils';
import { Language } from '@core/i18n/translation.model';

/**
 * How long ago an instant was, in the reader's words (`il y a 12 min`, `3 hr. ago`).
 *
 * @param instant - The past instant, as an ISO-8601 string.
 * @param now - The current time, in milliseconds.
 * @param language - The active language.
 * @returns The elapsed time, "now" under a minute.
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

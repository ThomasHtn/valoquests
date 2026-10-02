import { CAMPAIGN_TIME_ZONE } from '@core/date/date-time.constants';
import { resolveLocale } from '@core/i18n/locale.utils';
import { Language } from '@core/i18n/translation.model';
import { routePath } from '@core/navigation/navigation-history.utils';
import { SYNC_STALE_AFTER_MS } from './sidebar.constants';
import { NavItem } from './sidebar.model';

/**
 * Whether `item` should render as the active navigation entry for `url`.
 *
 * `exactMatch` entries only match the current URL outright; every other entry also matches a child
 * route under its own `routerLink`, and under any of its `activeRoutes` (a second page reached from
 * within the section rather than from the sidebar, which still shares this one entry).
 *
 * @param url - The current URL.
 * @param item - The navigation entry to check.
 * @returns Whether the entry is active for `url`.
 */
export function isNavItemActive(url: string, item: NavItem): boolean {
  const routes = [item.routerLink, ...(item.activeRoutes ?? [])].filter(
    (route): route is string => !!route,
  );

  // Query and fragment do not change the page: `/rules#streak` is still the rules.
  const path = routePath(url);
  return routes.some(
    (route) => path === route || (!item.exactMatch && path.startsWith(`${route}/`)),
  );
}

/**
 * Formats an ISO-8601 instant as a short date and time in the reader's notation, on the campaign's
 * clock (`02/10/2026 14:05`, `10/2/26, 2:05 PM`).
 *
 * @param instant - The instant to format, as an ISO-8601 string.
 * @param language - The active language.
 * @returns The formatted timestamp.
 */
export function formatSynchronizationTimestamp(instant: string, language: Language): string {
  return new Intl.DateTimeFormat(resolveLocale(language), {
    dateStyle: 'short',
    timeStyle: 'short',
    timeZone: CAMPAIGN_TIME_ZONE,
  }).format(new Date(instant));
}

/**
 * Whether the last completed synchronization is older than the cadence allows.
 *
 * @param lastCompletedAt - When it completed, as an ISO-8601 string.
 * @param now - The current time, in milliseconds.
 * @returns Whether it is late.
 */
export function isSynchronizationStale(lastCompletedAt: string, now: number): boolean {
  return now - Date.parse(lastCompletedAt) > SYNC_STALE_AFTER_MS;
}

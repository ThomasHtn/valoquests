import { CAMPAIGN_TIME_ZONE } from '@core/campaign/calendar/campaign-calendar.constants';
import { resolveLocale } from '@core/i18n/format/locale.utils';
import { Language } from '@core/i18n/translation.model';
import { routePath } from '@core/navigation/navigation-history.utils';
import { SYNC_STALE_AFTER_MS } from './sidebar.constants';
import { NavItem } from './sidebar.model';

/**
 * Whether `item` is active for `url`, child routes included unless `exactMatch`.
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
 * Short date and time of an ISO-8601 instant on the campaign clock (`02/10/2026 14:05`).
 */
export function formatSynchronizationTimestamp(instant: string, language: Language): string {
  return new Intl.DateTimeFormat(resolveLocale(language), {
    dateStyle: 'short',
    timeStyle: 'short',
    timeZone: CAMPAIGN_TIME_ZONE,
  }).format(new Date(instant));
}

/**
 * Whether the last synchronization (ISO-8601) is older than the cadence allows at `now` (ms).
 */
export function isSynchronizationStale(lastCompletedAt: string, now: number): boolean {
  return now - Date.parse(lastCompletedAt) > SYNC_STALE_AFTER_MS;
}

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
 * Whether the last synchronization (ISO-8601) is older than the cadence allows at `now` (ms).
 */
export function isSynchronizationStale(lastCompletedAt: string, now: number): boolean {
  return now - Date.parse(lastCompletedAt) > SYNC_STALE_AFTER_MS;
}

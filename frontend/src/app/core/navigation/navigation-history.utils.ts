import { BACK_LABEL_KEYS } from './navigation-history.constants';

/**
 * Router URL path without query string or fragment.
 */
export function routePath(url: string): string {
  return url.split(/[?#]/, 1)[0];
}

/**
 * Whether two router URLs name the same page, ignoring query and fragment.
 */
export function sameRoutePath(a: string, b: string): boolean {
  return routePath(a) === routePath(b);
}

/**
 * Translation key naming a URL's page for a back link, `null` when not named.
 */
export function resolveBackLabelKey(url: string): string | null {
  const path = routePath(url);
  return BACK_LABEL_KEYS.find((entry) => entry.pattern.test(path))?.key ?? null;
}

import { BACK_LABEL_KEYS } from './navigation-history.constants';

/**
 * The path of a router URL, without its query string or fragment.
 *
 * @param url - A router URL (`/players/3?view=progress#top`).
 * @returns Its path (`/players/3`).
 */
export function routePath(url: string): string {
  return url.split(/[?#]/, 1)[0];
}

/**
 * Whether two router URLs name the same page, whatever their query string or fragment.
 */
export function sameRoutePath(a: string, b: string): boolean {
  return routePath(a) === routePath(b);
}

/**
 * The translation key naming the page a URL leads to, for a back link pointing at it.
 *
 * @param url - A router URL.
 * @returns The key, or `null` for a page a back link does not name.
 */
export function resolveBackLabelKey(url: string): string | null {
  const path = routePath(url);
  return BACK_LABEL_KEYS.find((entry) => entry.pattern.test(path))?.key ?? null;
}

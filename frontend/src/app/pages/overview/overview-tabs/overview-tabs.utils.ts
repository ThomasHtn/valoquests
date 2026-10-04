import { TranslateFn } from '@core/i18n/translation.model';
import { FAVORITE_TAB_KEY, OVERVIEW_TABS } from './overview-tabs.constants';
import { OverviewTab, OverviewTabKey } from './overview-tabs.model';

/**
 * Tab named in the address (`?tab=campaign`), `null` for anything else.
 */
export function parseOverviewTab(value: string | null): OverviewTabKey | null {
  return OVERVIEW_TABS.find((key) => key === value) ?? null;
}

/**
 * Reader's pinned default tab, `null` when none.
 */
export function readFavoriteTab(): OverviewTabKey | null {
  try {
    const stored = localStorage.getItem(FAVORITE_TAB_KEY);
    return OVERVIEW_TABS.find((key) => key === stored) ?? null;
  } catch {
    return null;
  }
}

/**
 * Stores the default tab, clears it on `null`; storage failures are ignored.
 */
export function writeFavoriteTab(key: OverviewTabKey | null): void {
  try {
    if (key === null) {
      localStorage.removeItem(FAVORITE_TAB_KEY);
    } else {
      localStorage.setItem(FAVORITE_TAB_KEY, key);
    }
  } catch {
    // The page opens on its first tab next time.
  }
}

/**
 * Tabs in bar order, the pinned one (if any) leading.
 */
export function buildTabs(
  translate: TranslateFn,
  favorite: OverviewTabKey | null = null,
): readonly OverviewTab[] {
  const keys = favorite
    ? [favorite, ...OVERVIEW_TABS.filter((key) => key !== favorite)]
    : OVERVIEW_TABS;
  return keys.map((key) => ({ key, label: translate(`overview.tabs.${key}.label`) }));
}

import { describe, expect, it } from 'vitest';

import { NavItem } from './sidebar.model';
import { isNavItemActive } from './sidebar.utils';

function item(overrides: Partial<NavItem> = {}): NavItem {
  return {
    labelKey: 'overview',
    icon: 'layout-dashboard',
    routerLink: '/overview',
    ...overrides,
  };
}

describe('isNavItemActive', () => {
  it('matches the exact route', () => {
    expect(isNavItemActive('/overview', item())).toBe(true);
  });

  it('matches a child route by default', () => {
    expect(isNavItemActive('/players/42', item({ routerLink: '/players' }))).toBe(true);
  });

  it('does not match a sibling route sharing the same prefix', () => {
    expect(isNavItemActive('/playersomething', item({ routerLink: '/players' }))).toBe(false);
  });

  it('restricts an exactMatch entry to the exact route, never a child one', () => {
    const rootItem = item({ routerLink: '/', exactMatch: true });

    expect(isNavItemActive('/', rootItem)).toBe(true);
    expect(isNavItemActive('/overview', rootItem)).toBe(false);
  });

  it('also matches one of the entry’s extra active routes', () => {
    const campaignItem = item({ routerLink: '/campaign', activeRoutes: ['/campaign/history'] });

    expect(isNavItemActive('/campaign/history', campaignItem)).toBe(true);
    expect(isNavItemActive('/campaign/history/2', campaignItem)).toBe(true);
  });

  it('never matches an entry with no route at all', () => {
    expect(isNavItemActive('/overview', item({ routerLink: undefined }))).toBe(false);
  });
});

import { NavItem } from './sidebar.model';

/**
 * Primary navigation entries, in display order.
 *
 * Kept as data rather than repeated markup so the shared layout and long
 * Tailwind class list are written once and iterated with `@for`. Entries
 * without a `routerLink` have no page yet and render as inert.
 */
export const NAV_ITEMS: readonly NavItem[] = [
  { labelKey: 'overview', icon: 'layout-dashboard', routerLink: '/overview', exactMatch: true },
  { labelKey: 'challenges', icon: 'target', routerLink: '/challenges' },
  { labelKey: 'boss', icon: 'skull', routerLink: '/boss' },
  { labelKey: 'leaderboard', icon: 'trophy', routerLink: '/leaderboard' },
  { labelKey: 'players', icon: 'users', routerLink: '/players' },
  { labelKey: 'rules', icon: 'book-open', routerLink: '/rules' },
];

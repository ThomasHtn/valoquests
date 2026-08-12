import { NavItem } from './sidebar.model';

/**
 * Primary navigation entries, in display order.
 *
 * Kept as data rather than repeated markup so the shared layout and long
 * Tailwind class list are written once and iterated with `@for`. Entries
 * without a `routerLink` have no page yet and render as inert.
 */
export const NAV_ITEMS: readonly NavItem[] = [
  { labelKey: 'overview', routerLink: '/overview', exactMatch: true },
  { labelKey: 'challenges', routerLink: '/challenges' },
  { labelKey: 'boss', routerLink: '/boss' },
  { labelKey: 'leaderboard', routerLink: '/leaderboard' },
  { labelKey: 'players', routerLink: '/players' },
  //{ labelKey: 'comparison' },
  { labelKey: 'rules', routerLink: '/rules' },
];

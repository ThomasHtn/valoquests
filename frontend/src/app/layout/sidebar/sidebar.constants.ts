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
  { labelKey: 'boss', icon: 'skull', routerLink: '/campaign' },
  { labelKey: 'leaderboard', icon: 'trophy', routerLink: '/leaderboard' },
  { labelKey: 'players', icon: 'users', routerLink: '/players' },
  { labelKey: 'rules', icon: 'book-open', routerLink: '/rules' },
];

/**
 * Navigation entries shown instead of {@link NAV_ITEMS} while a backoffice session is open.
 *
 * The backoffice replaces the navigation rather than adding to it: the coach signing in is there to
 * operate the tracker, not to browse it, and the two sets of destinations have nothing to do with
 * one another. The public pages stay reachable by URL throughout — the swap is about what the
 * sidebar offers, not about locking anything away.
 */
export const ADMIN_NAV_ITEMS: readonly NavItem[] = [
  { labelKey: 'adminOperations', icon: 'refresh-cw', routerLink: '/admin/operations' },
  { labelKey: 'adminPlayers', icon: 'user-cog', routerLink: '/admin/players' },
  { labelKey: 'adminMaintenance', icon: 'database-backup', routerLink: '/admin/maintenance' },
  { labelKey: 'adminDesignSystem', icon: 'palette', routerLink: '/admin/design-system' },
];

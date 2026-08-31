import { NavItem } from './sidebar.model';

/**
 * Version of the application, shown at the very bottom of the sidebar.
 *
 * Written here rather than read from `package.json`: the manifest's version is never bumped for a
 * deployment of this personal project, so the displayed one would always lie.
 */
export const APP_VERSION = '1.0-beta';

/**
 * Primary navigation entries, in display order.
 *
 * Kept as data rather than repeated markup so the shared layout and long
 * Tailwind class list are written once and iterated with `@for`. Entries
 * without a `routerLink` have no page yet and render as inert.
 *
 * The order carries the game's two loops rather than an alphabet: the week first — what to do
 * (challenges) then who is winning it (leaderboard) — and the run after it, since the campaign is
 * what those weeks add up to. Splitting them under section headers was considered and dropped: six
 * entries do not need chapters, and the collapsed rail shows icons only, where a header has nothing
 * to render.
 */
export const NAV_ITEMS: readonly NavItem[] = [
  { labelKey: 'overview', icon: 'layout-dashboard', routerLink: '/overview', exactMatch: true },
  { labelKey: 'challenges', icon: 'target', routerLink: '/week' },
  { labelKey: 'leaderboard', icon: 'trophy', routerLink: '/leaderboard' },
  { labelKey: 'campaign', icon: 'map', routerLink: '/campaign' },
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
  { labelKey: 'adminCampaigns', icon: 'flag', routerLink: '/admin/campaigns' },
  { labelKey: 'adminMaintenance', icon: 'database-backup', routerLink: '/admin/maintenance' },
];

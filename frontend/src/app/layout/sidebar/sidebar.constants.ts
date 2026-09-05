import { NavGroup } from './sidebar.model';

/**
 * Version of the application, shown at the very bottom of the sidebar.
 *
 * Written here rather than read from `package.json`: the manifest's version is never bumped for a
 * deployment of this personal project, so the displayed one would always lie.
 */
export const APP_VERSION = '1.0-beta';

/**
 * Primary navigation, in display order, chaptered the way the game reads: the expedition (the
 * week's home, its challenges, the campaign they add up to), then the squad (who is winning, who
 * is in it), then help. On the collapsed rail the captions have nothing to render: a hairline
 * stands in for each break.
 *
 * Kept as data rather than repeated markup so the shared layout and long Tailwind class list are
 * written once and iterated with `@for`. Entries without a `routerLink` have no page yet and
 * render as inert.
 */
export const NAV_GROUPS: readonly NavGroup[] = [
  {
    labelKey: 'expedition',
    items: [
      { labelKey: 'overview', icon: 'layout-dashboard', routerLink: '/overview', exactMatch: true },
      { labelKey: 'challenges', icon: 'target', routerLink: '/challenges' },
      { labelKey: 'campaign', icon: 'map', routerLink: '/campaign' },
    ],
  },
  {
    labelKey: 'squad',
    items: [
      { labelKey: 'leaderboard', icon: 'trophy', routerLink: '/leaderboard' },
      { labelKey: 'players', icon: 'users', routerLink: '/players' },
    ],
  },
  {
    labelKey: 'help',
    items: [{ labelKey: 'rules', icon: 'book-open', routerLink: '/rules' }],
  },
];

/**
 * Navigation shown instead of {@link NAV_GROUPS} while a backoffice session is open.
 *
 * The backoffice replaces the navigation rather than adding to it: the coach signing in is there to
 * operate the tracker, not to browse it, and the two sets of destinations have nothing to do with
 * one another. The public pages stay reachable by URL throughout — the swap is about what the
 * sidebar offers, not about locking anything away.
 */
export const ADMIN_NAV_GROUPS: readonly NavGroup[] = [
  {
    labelKey: 'admin',
    items: [
      { labelKey: 'adminOperations', icon: 'refresh-cw', routerLink: '/admin/operations' },
      { labelKey: 'adminPlayers', icon: 'user-cog', routerLink: '/admin/players' },
      { labelKey: 'adminCampaigns', icon: 'flag', routerLink: '/admin/campaigns' },
      { labelKey: 'adminMaintenance', icon: 'database-backup', routerLink: '/admin/maintenance' },
    ],
  },
];

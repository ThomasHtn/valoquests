import { NavGroup } from './sidebar.model';

/**
 * Primary navigation chapters, in display order.
 */
export const NAV_GROUPS: readonly NavGroup[] = [
  {
    labelKey: 'expedition',
    items: [
      { labelKey: 'overview', icon: 'layout-dashboard', routerLink: '/overview', exactMatch: true },
      { labelKey: 'challenges', icon: 'target', routerLink: '/challenges' },
    ],
  },
  {
    labelKey: 'operators',
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
 * Navigation replacing {@link NAV_GROUPS} while a backoffice session is open.
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

/**
 * Age past which the last synchronization reads as late: 30-minute cadence plus a margin.
 */
export const SYNC_STALE_AFTER_MS = 40 * 60_000;

/**
 * Refresh period of the elapsed synchronization time.
 */
export const SIDEBAR_CLOCK_MS = 30_000;

/**
 * Utilities of the active navigation entry.
 */
export const NAV_ACTIVE_CLASS = ' bg-brand-500/12 text-brand-500 before:bg-brand-500';

/**
 * Utilities of the open language trigger: a stronger hover state.
 */
export const LANGUAGE_MENU_OPEN_CLASS = 'bg-brand-500/8 text-brand-500';

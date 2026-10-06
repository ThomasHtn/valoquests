import {
  LucideBookOpen,
  LucideDatabaseBackup,
  LucideFlag,
  LucideLayoutDashboard,
  LucideRefreshCw,
  LucideTarget,
  LucideTrophy,
  LucideUserCog,
  LucideUsers,
} from '@lucide/angular';

import { NavGroup } from './sidebar.model';

/**
 * Primary navigation chapters, in display order.
 */
export const NAV_GROUPS: readonly NavGroup[] = [
  {
    labelKey: 'expedition',
    items: [
      {
        labelKey: 'overview',
        icon: LucideLayoutDashboard,
        routerLink: '/overview',
        exactMatch: true,
      },
      { labelKey: 'challenges', icon: LucideTarget, routerLink: '/challenges' },
    ],
  },
  {
    labelKey: 'operators',
    items: [
      { labelKey: 'leaderboard', icon: LucideTrophy, routerLink: '/leaderboard' },
      { labelKey: 'players', icon: LucideUsers, routerLink: '/players' },
    ],
  },
  {
    labelKey: 'help',
    items: [{ labelKey: 'rules', icon: LucideBookOpen, routerLink: '/rules' }],
  },
];

/**
 * Navigation replacing {@link NAV_GROUPS} while a backoffice session is open.
 */
export const ADMIN_NAV_GROUPS: readonly NavGroup[] = [
  {
    labelKey: 'admin',
    items: [
      { labelKey: 'adminOperations', icon: LucideRefreshCw, routerLink: '/admin/operations' },
      { labelKey: 'adminPlayers', icon: LucideUserCog, routerLink: '/admin/players' },
      { labelKey: 'adminCampaigns', icon: LucideFlag, routerLink: '/admin/campaigns' },
      {
        labelKey: 'adminMaintenance',
        icon: LucideDatabaseBackup,
        routerLink: '/admin/maintenance',
      },
    ],
  },
];

/**
 * Age past which the last synchronization reads as late: 5-minute cadence plus two missed runs.
 */
export const SYNC_STALE_AFTER_MS = 15 * 60_000;

/**
 * Refresh period of the elapsed synchronization time.
 */
export const SIDEBAR_CLOCK_MS = 30_000;

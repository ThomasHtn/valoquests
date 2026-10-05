import type { LucideIcon } from '@lucide/angular';
import {
  LucideArrowDownAZ,
  LucideArrowDownNarrowWide,
  LucideArrowDownWideNarrow,
  LucideArrowDownZA,
} from '@lucide/angular';

import { PlayerSortColumn, PlayerSortOrder } from './players.model';

/**
 * Query parameters holding the sort (`/players?sort=kda&dir=asc`).
 */
export const PLAYER_SORT_PARAMS = { key: 'sort', direction: 'dir' } as const;

/**
 * Sortable columns, in display order.
 */
export const PLAYER_SORT_COLUMNS: readonly PlayerSortColumn[] = [
  { key: 'name', labelKey: 'players.columns.player', align: 'left', helpKey: null },
  {
    key: 'rank',
    labelKey: 'players.columns.rank',
    align: 'left',
    helpKey: 'players.columns.rankHelp',
  },
  {
    key: 'winRate',
    labelKey: 'players.columns.winRate',
    align: 'left',
    helpKey: 'playerProfile.stats.tooltip.winRate',
  },
  {
    key: 'kda',
    labelKey: 'players.columns.kda',
    align: 'right',
    helpKey: 'playerProfile.stats.tooltip.kda',
  },
  {
    key: 'headshotPercentage',
    labelKey: 'players.columns.headshotPercentage',
    align: 'right',
    helpKey: 'playerProfile.stats.tooltip.headshotPercentage',
  },
  {
    key: 'matchesPlayed',
    labelKey: 'players.columns.matchesPlayed',
    align: 'right',
    helpKey: 'players.columns.matchesHelp',
  },
];

/**
 * Arrow of the phone's sort toggle per order: letters for names, bar widths for figures.
 */
export const PLAYER_SORT_ORDER_ICONS: Readonly<Record<PlayerSortOrder, LucideIcon>> = {
  az: LucideArrowDownAZ,
  za: LucideArrowDownZA,
  best: LucideArrowDownWideNarrow,
  worst: LucideArrowDownNarrowWide,
  high: LucideArrowDownWideNarrow,
  low: LucideArrowDownNarrowWide,
};

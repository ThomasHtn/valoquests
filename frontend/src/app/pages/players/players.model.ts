import { CompetitiveTier, CompetitiveTierVisual } from '@core/players/competitive-tier.model';

/**
 * Single row of the players table: a tracked player mapped to display-ready fields.
 */
export interface PlayerRow {
  readonly id: number;
  readonly displayName: string;

  /**
   * Whether this player holds the reigning weekly "Champion" title, earned by finishing 1st in
   * the most recently finalized week.
   */
  readonly isChampion: boolean;

  /**
   * Tag segment of the player's Riot ID (e.g. `"EUW"` from `"Kenshiro#EUW"`), or `null` when
   * absent.
   */
  readonly tag: string | null;
  readonly avatarUrl: string | null;

  /**
   * The tier's raw enum value, kept alongside {@link tier}'s translated label — sorting needs the
   * former (a stable, orderable value), the template only ever the latter.
   */
  readonly competitiveTier: CompetitiveTier;
  readonly tier: CompetitiveTierVisual;
  readonly rankIconUrl: string | null;
  readonly rankRating: number | null;
  readonly winRate: number | null;
  readonly kda: number | null;
  readonly headshotPercentage: number | null;
  readonly matchesPlayed: number;

  /**
   * Whether this player currently takes part in the campaign (`PlayerStatus.ACTIVE`). `false`
   * groups the row under "hors campagne" instead of the roster proper — see root `CLAUDE.md`.
   */
  readonly inCampaign: boolean;
}

/**
 * One column the table can be sorted on.
 */
export type PlayerSortKey =
  'name' | 'rank' | 'winRate' | 'kda' | 'headshotPercentage' | 'matchesPlayed';

/**
 * One sortable header of the table: a column paired with its translation key and text alignment.
 */
export interface PlayerSortColumn {
  readonly key: PlayerSortKey;
  readonly labelKey: string;
  readonly align: 'left' | 'right';
}

/**
 * The table's sortable columns, in display order.
 */
export const PLAYER_SORT_COLUMNS: readonly PlayerSortColumn[] = [
  { key: 'name', labelKey: 'players.columns.player', align: 'left' },
  { key: 'rank', labelKey: 'players.columns.rank', align: 'left' },
  { key: 'winRate', labelKey: 'players.columns.winRate', align: 'left' },
  { key: 'kda', labelKey: 'players.columns.kda', align: 'right' },
  { key: 'headshotPercentage', labelKey: 'players.columns.headshotPercentage', align: 'right' },
  { key: 'matchesPlayed', labelKey: 'players.columns.matchesPlayed', align: 'right' },
];

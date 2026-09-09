import { TitleVisual } from '@core/campaign/campaign-visual.utils';
import { WeeklyTitle } from '@core/campaign/campaign.model';
import { CompetitiveTier, CompetitiveTierVisual } from '@core/players/competitive-tier.model';

/**
 * Single row of the players table: a tracked player mapped to display-ready fields.
 */
export interface PlayerRow {
  /**
   * Internal identifier.
   */
  readonly id: number;

  /**
   * Name shown across the application.
   */
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

  /**
   * Resolved avatar URL, or `null` when the player has none.
   */
  readonly avatarUrl: string | null;

  /**
   * The one weekly title this player is decorated with this week, or `null` when they hold none.
   */
  readonly title: (TitleVisual & { readonly key: WeeklyTitle }) | null;

  /**
   * The tier's raw enum value, kept alongside {@link tier}'s translated label — sorting needs the
   * former (a stable, orderable value), the template only ever the latter.
   */
  readonly competitiveTier: CompetitiveTier;

  /**
   * Translated rank label and colour.
   */
  readonly tier: CompetitiveTierVisual;

  /**
   * Icon of the competitive rank, or `null` when unranked.
   */
  readonly rankIconUrl: string | null;

  /**
   * Rank rating points within the tier, or `null` when unranked.
   */
  readonly rankRating: number | null;

  /**
   * Win rate in percent, or `null` without matches.
   */
  readonly winRate: number | null;

  /**
   * KDA, or `null` without matches.
   */
  readonly kda: number | null;

  /**
   * Headshot rate in percent, or `null` without matches.
   */
  readonly headshotPercentage: number | null;

  /**
   * Matches played.
   */
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
  /**
   * Column the header sorts on.
   */
  readonly key: PlayerSortKey;

  /**
   * Translation key of the header.
   */
  readonly labelKey: string;

  /**
   * Text alignment of the column.
   */
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

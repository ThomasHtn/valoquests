import { WeeklyTitle } from '@core/campaign/titles/campaign-title.model';
import {
  CompetitiveTier,
  CompetitiveTierVisual,
} from '@core/players/competitive-tier/player-competitive-tier.model';
import { StatVisual } from '@core/players/stats/player-stats.model';

/**
 * One row of the players table, ready to display.
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
   * Whether the player won the last finalized week.
   */
  readonly isChampion: boolean;

  /**
   * Riot ID tag (`"EUW"` in `"Kenshiro#EUW"`), `null` when absent.
   */
  readonly tag: string | null;

  /**
   * Avatar URL, `null` when the player has none.
   */
  readonly avatarUrl: string | null;

  /**
   * Weekly title held this week, `null` when none.
   */
  readonly title: WeeklyTitle | null;

  /**
   * Raw tier, kept for sorting since `tier` only holds the label.
   */
  readonly competitiveTier: CompetitiveTier;

  /**
   * Translated rank label and colour.
   */
  readonly tier: CompetitiveTierVisual;

  /**
   * Rank icon, including the unranked badge.
   */
  readonly rankIconUrl: string | null;

  /**
   * Rank rating within the tier, `null` when unranked.
   */
  readonly rankRating: number | null;

  /**
   * Win rate in percent, `null` without matches.
   */
  readonly winRate: number | null;

  /**
   * Formatted win rate.
   */
  readonly winRateLabel: string;

  /**
   * Win rate text and bar colours.
   */
  readonly winRateVisual: StatVisual;

  /**
   * KDA, `null` without matches.
   */
  readonly kda: number | null;

  /**
   * Formatted KDA.
   */
  readonly kdaLabel: string;

  /**
   * KDA text colour.
   */
  readonly kdaVisual: StatVisual;

  /**
   * Headshot rate in percent, `null` without matches.
   */
  readonly headshotPercentage: number | null;

  /**
   * Formatted headshot rate.
   */
  readonly headshotPercentageLabel: string;

  /**
   * Matches played.
   */
  readonly matchesPlayed: number;

  /**
   * Whether the player is `ACTIVE`; `false` lists them under "hors campagne".
   */
  readonly inCampaign: boolean;
}

/**
 * Column the table can be sorted on.
 */
export type PlayerSortKey =
  'name' | 'rank' | 'winRate' | 'kda' | 'headshotPercentage' | 'matchesPlayed';

/**
 * Sort order as worded by the phone's toggle.
 */
export type PlayerSortOrder = 'az' | 'za' | 'best' | 'worst' | 'high' | 'low';

/**
 * Sortable table header.
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

  /**
   * Translation key of the tooltip and phone legend, `null` for none.
   */
  readonly helpKey: string | null;
}

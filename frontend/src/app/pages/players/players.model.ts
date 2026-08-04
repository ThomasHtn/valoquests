import { CompetitiveTierVisual } from '@core/players/competitive-tier.model';

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
  readonly tier: CompetitiveTierVisual;
  readonly rankIconUrl: string | null;
  readonly rankRating: number | null;
  readonly winRate: number | null;
  readonly kda: number | null;
  readonly headshotPercentage: number | null;
  readonly matchesPlayed: number;
}

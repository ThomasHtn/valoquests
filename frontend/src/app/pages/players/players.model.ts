/**
 * Resolved visual treatment for a player's competitive tier: a translated label (e.g.
 * `"Diamond 1"`) paired with the color class shared by its badge and text.
 */
export interface CompetitiveTierVisual {
  readonly label: string;
  readonly colorClass: string;
}

/**
 * Single row of the players table: a tracked player mapped to display-ready fields.
 */
export interface PlayerRow {
  readonly id: number;
  readonly displayName: string;

  /**
   * Tag segment of the player's Riot ID (e.g. `"EUW"` from `"Kenshiro#EUW"`), or `null` when
   * absent.
   */
  readonly tag: string | null;
  readonly avatarUrl: string | null;
  readonly tier: CompetitiveTierVisual;
  readonly rankRating: number | null;
  readonly winRate: number | null;
  readonly kda: number | null;
  readonly headshotPercentage: number | null;
  readonly matchesPlayed: number;
}

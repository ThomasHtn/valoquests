/**
 * Single historical week's boss confrontation, mapped to display-ready fields.
 */
export interface BossHistoryRow {
  readonly weekStart: string;
  readonly weekLabel: string;
  readonly dateRangeLabel: string;
  readonly bossName: string;
  readonly bossDescription: string;
  readonly categoryColorClass: string;
  readonly categoryLabel: string;
  readonly effectiveHp: number;
  readonly totalDamageDealt: number;
  readonly defeated: boolean;
  readonly defeatedByPlayerId: number | null;
  readonly defeatedByPlayerDisplayName: string | null;
  readonly defeatedByAvatarUrl: string | null;

  /**
   * Whether the player who dealt the finishing blow holds the reigning weekly "Champion" title,
   * earned by finishing 1st in the most recently finalized week.
   */
  readonly defeatedByIsChampion: boolean;
  readonly winStreak: number;
}

/**
 * Single row of a historical week's ranking table: a player mapped to display-ready fields.
 */
export interface RankingHistoryRow {
  readonly position: number;
  readonly playerId: number;
  readonly displayName: string;
  readonly avatarUrl: string | null;
  readonly points: number;
  readonly completedChallenges: number;
}

/**
 * Single historical week rendered as its own ranking block, paired with its resolved label.
 */
export interface RankingHistoryWeekView {
  readonly weekStart: string;
  readonly weekLabel: string;
  readonly dateRangeLabel: string;
  readonly rows: readonly RankingHistoryRow[];
}

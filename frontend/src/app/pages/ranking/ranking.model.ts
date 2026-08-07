/**
 * Single row of a historical week's ranking table: a player mapped to display-ready fields.
 */
export interface RankingHistoryRow {
  readonly position: number;
  readonly playerId: number;
  readonly displayName: string;
  readonly avatarUrl: string | null;
  readonly damage: number;
  readonly completedChallenges: number;
}

/**
 * Single historical week rendered as its own ranking block, paired with its resolved label.
 *
 * Split into a podium spotlight and the rest of the field, mirroring the overview page's podium so
 * a finalized week reads the same competitive shape as the live one.
 */
export interface RankingHistoryWeekView {
  readonly weekStart: string;
  readonly weekLabel: string;
  readonly dateRangeLabel: string;

  /**
   * The top 3 finishers, shown as the week's podium spotlight.
   */
  readonly top3: readonly RankingHistoryRow[];

  /**
   * Finishers from 4th place onward, shown as a compact list below the podium.
   */
  readonly rest: readonly RankingHistoryRow[];
}

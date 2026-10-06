import { WeeklyTitle } from '@core/campaign/titles/campaign-title.model';
import { CompetitiveTier } from '@core/players/competitive-tier/player-competitive-tier.model';

/**
 * Player identity and rank of a ranking entry. Mirrors the backend `PlayerRankingResponse`.
 */
interface PlayerRanking {
  /**
   * Player identifier.
   */
  readonly id: number;

  /**
   * Display name.
   */
  readonly displayName: string;

  /**
   * Portrait path or URL, `null` until synchronized.
   */
  readonly portrait: string | null;

  /**
   * Competitive rank held.
   */
  readonly competitiveTier: CompetitiveTier;

  /**
   * Rank rating within the tier, `null` when unranked.
   */
  readonly rankRating: number | null;
}

/**
 * One row of the current weekly ranking. Mirrors the backend `RankingEntryResponse`.
 */
export interface RankingEntry {
  /**
   * 1-based position, shared on ties, `null` without points or when inactive.
   */
  readonly position: number | null;

  /**
   * `false` for an inactive player, out of the ranking.
   */
  readonly competing: boolean;

  /**
   * Places gained (positive) or lost since the previous calculation.
   */
  readonly positionVariation: number;

  /**
   * The ranked player.
   */
  readonly player: PlayerRanking;

  /**
   * Damage to the week's guardian, streak bonus included.
   */
  readonly guardianDamage: number;

  /**
   * Food.
   */
  readonly food: number;

  /**
   * Components.
   */
  readonly components: number;

  /**
   * Matches played.
   */
  readonly matchCount: number;

  /**
   * Days played this week so far.
   */
  readonly playedDays: number;

  /**
   * Points from this week's validated challenges, weekly and daily.
   */
  readonly challengePoints: number;

  /**
   * Weekly challenges validated, out of `totalChallenges`.
   */
  readonly completedChallenges: number;

  /**
   * Challenges drawn this week.
   */
  readonly totalChallenges: number;

  /**
   * Daily challenges validated this week.
   */
  readonly completedDailyChallenges: number;

  /**
   * Guardian damage plus challenge points, the ranking order.
   */
  readonly totalPoints: number;

  /**
   * Titles the player holds on the week so far.
   */
  readonly titles: readonly WeeklyTitle[];
}

/**
 * Current weekly ranking. Mirrors the backend `CurrentRankingResponse`.
 */
export interface CurrentRanking {
  /**
   * Monday of the active week (`YYYY-MM-DD`).
   */
  readonly weekStart: string;

  /**
   * Sunday of the active week (`YYYY-MM-DD`).
   */
  readonly weekEnd: string;

  /**
   * Day in progress (`YYYY-MM-DD`).
   */
  readonly today: string;

  /**
   * Entries in board order.
   */
  readonly ranking: readonly RankingEntry[];
}

/**
 * One player's day. Mirrors the backend `DailyRankingEntryResponse`.
 */
export interface DailyRankingEntry {
  /**
   * 1-based rank, shared on ties, `null` without damage or when inactive.
   */
  readonly position: number | null;

  /**
   * `false` for an inactive player, out of the ranking.
   */
  readonly competing: boolean;

  /**
   * Player identifier.
   */
  readonly playerId: number;

  /**
   * Display name.
   */
  readonly displayName: string;

  /**
   * Portrait path or URL, `null` until synchronized.
   */
  readonly portrait: string | null;

  /**
   * Day damage, diminishing returns and streak bonus applied.
   */
  readonly damage: number;

  /**
   * Food.
   */
  readonly food: number;

  /**
   * Components.
   */
  readonly components: number;

  /**
   * Matches played.
   */
  readonly matchCount: number;

  /**
   * Matches reduced by the day's diminishing returns.
   */
  readonly reducedMatchCount: number;

  /**
   * Days played this week up to this day included, `0` when not played.
   */
  readonly playedDays: number;

  /**
   * Bonus the days played this week grant, in percent.
   */
  readonly streakBonusPercent: number;

  /**
   * Week days played up to this day included, ascending ISO dates.
   */
  readonly weekPlayedDays: readonly string[];
}

/**
 * One day's ranking, priced on demand. Mirrors the backend `DailyRankingResponse`.
 */
export interface DailyRanking {
  /**
   * Day on the board (`YYYY-MM-DD`).
   */
  readonly day: string;

  /**
   * Competing players, deactivated and archived ones excluded.
   */
  readonly rosterPlayerCount: number;

  /**
   * One entry per non-archived roster player, deactivated ones at a `null` position.
   */
  readonly ranking: readonly DailyRankingEntry[];
}

/**
 * A player's final result in a past week. Mirrors the backend `FinalRankingEntryResponse`.
 */
export interface RankingHistoryEntry {
  /**
   * Position on the board, first being 1.
   */
  readonly position: number;

  /**
   * Player identifier.
   */
  readonly playerId: number;

  /**
   * Display name.
   */
  readonly displayName: string;

  /**
   * Guardian damage dealt this week.
   */
  readonly guardianDamage: number;

  /**
   * Ranking points earned from challenges.
   */
  readonly challengePoints: number;

  /**
   * Frozen weekly total the final position was ordered on.
   */
  readonly totalPoints: number;

  /**
   * Weekly challenges validated this week.
   */
  readonly completedChallenges: number;

  /**
   * Daily challenges validated this week.
   */
  readonly completedDailyChallenges: number;

  /**
   * Matches played that week.
   */
  readonly matchCount: number;

  /**
   * Days played that week.
   */
  readonly playedDays: number;

  /**
   * Weekly titles held.
   */
  readonly titles: readonly WeeklyTitle[];
}

/**
 * Frozen ranking of a completed week. Mirrors the backend `RankingHistoryWeekResponse`.
 */
export interface RankingHistoryWeek {
  /**
   * Monday of the week (`YYYY-MM-DD`).
   */
  readonly weekStart: string;

  /**
   * Sunday of the week (`YYYY-MM-DD`).
   */
  readonly weekEnd: string;

  /**
   * Winner, `null` when nobody was ranked.
   */
  readonly winnerPlayerId: number | null;

  /**
   * Final ranking of the closed week.
   */
  readonly ranking: readonly RankingHistoryEntry[];
}

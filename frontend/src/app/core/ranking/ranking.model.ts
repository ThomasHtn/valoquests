import { WeeklyTitle } from '@core/campaign/campaign.model';
import { ChallengeCadence, ChallengeDifficulty } from '@core/challenges/challenge.model';
import { CompetitiveTier } from '@core/players/competitive-tier.model';

/**
 * Player identity and rank data attached to a ranking entry.
 *
 * Mirrors `CurrentRankingResponse.PlayerRankingResponse` from the backend.
 */
export interface PlayerRanking {
  /**
   * Internal identifier of the player.
   */
  readonly id: number;

  /**
   * Name shown across the application.
   */
  readonly displayName: string;

  /**
   * Relative path or URL of the player portrait, or `null` when not yet synchronized.
   */
  readonly portrait: string | null;

  /**
   * Competitive rank held.
   */
  readonly competitiveTier: CompetitiveTier;

  /**
   * Rank rating points within the tier, or `null` when unranked.
   */
  readonly rankRating: number | null;
}

/**
 * A player's exact progress toward one challenge on the week's board: one of the five weekly
 * ones, or today's daily.
 *
 * Mirrors `CurrentRankingResponse.ChallengeProgressResponse` from the backend.
 */
export interface RankingChallengeProgress {
  /**
   * Identifier of the challenge.
   */
  readonly id: number;

  /**
   * Stable catalogue code of the challenge.
   */
  readonly code: string;

  /**
   * Translated name of the challenge.
   */
  readonly name: string;

  /**
   * Whether the challenge is daily or weekly.
   */
  readonly cadence: ChallengeCadence;

  /**
   * Difficulty of a weekly challenge, `null` for the daily.
   */
  readonly difficulty: ChallengeDifficulty | null;

  /**
   * Day the daily is decided on, as an ISO-8601 date (`YYYY-MM-DD`); `null` for a weekly.
   */
  readonly day: string | null;

  /**
   * Metric the challenge measures.
   */
  readonly metric: string;

  /**
   * Progress so far.
   */
  readonly currentValue: number;

  /**
   * Value to reach, or `null` for an open-ended challenge.
   */
  readonly targetValue: number | null;

  /**
   * Unit of the values.
   */
  readonly unit: string;

  /**
   * Whether the challenge is validated.
   */
  readonly completed: boolean;

  /**
   * Points the challenge adds to the ranking once validated.
   */
  readonly rankingPoints: number;
}

/**
 * Single row of the current weekly ranking: a player's position, what they produced this week,
 * and their exact progress toward every challenge on the board.
 *
 * Mirrors `CurrentRankingResponse.RankingEntryResponse` from the backend.
 */
export interface RankingEntry {
  /**
   * 1-based ranking position, shared on equal points, or `null` when the player has no points
   * yet or is inactive and therefore never consumes a ranking slot.
   */
  readonly position: number | null;

  /**
   * Whether the player takes part in the ranking at all: `false` for an inactive player.
   */
  readonly competing: boolean;

  /**
   * Position at the previous calculation, or `null` when new.
   */
  readonly previousPosition: number | null;

  /**
   * Places gained (positive) or lost since the previous calculation.
   */
  readonly positionVariation: number;

  /**
   * The ranked player.
   */
  readonly player: PlayerRanking;

  /**
   * Damage dealt to the week's guardian by the matches that counted, streak bonus included.
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
   * Days with at least one match this week.
   */
  readonly activeDays: number;

  /**
   * Consecutive days played, counted up to the last day of the week played so far.
   */
  readonly streakDays: number;

  /**
   * Points banked by the challenges validated this week, weekly and daily alike.
   */
  readonly challengePoints: number;

  /**
   * Weekly challenges validated, over {@link totalChallenges}.
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
   * {@link guardianDamage} + {@link challengePoints}: what the ranking is ordered on.
   */
  readonly totalPoints: number;

  /**
   * Titles the player holds on the week so far.
   */
  readonly titles: readonly WeeklyTitle[];

  /**
   * One line per challenge on the board: the five weekly ones and today's daily.
   */
  readonly challengeProgress: readonly RankingChallengeProgress[];
}

/**
 * Current weekly ranking, with each player's exact progress toward every active challenge.
 *
 * Mirrors the backend `CurrentRankingResponse` returned by `GET /api/rankings/current`.
 */
export interface CurrentRanking {
  /**
   * Monday identifying the active week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly weekStart: string;

  /**
   * Sunday identifying the active week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly weekEnd: string;

  /**
   * The day in progress, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly today: string;

  /**
   * Instant at which this ranking was last calculated, as an ISO-8601 instant, or `null` before
   * the week's first calculation.
   */
  readonly calculatedAt: string | null;

  /**
   * Entries in board order.
   */
  readonly ranking: readonly RankingEntry[];
}

/**
 * Single player's day: what they brought in, and how that compares to the day before.
 *
 * Mirrors `DailyRankingResponse.DailyRankingEntryResponse` from the backend.
 */
export interface DailyRankingEntry {
  /**
   * 1-based rank on the day, shared on equal damage, or `null` when the player dealt none or is
   * inactive — same rule as {@link RankingEntry.position}.
   */
  readonly position: number | null;

  /**
   * Whether the player takes part in the ranking at all: `false` for an inactive player.
   */
  readonly competing: boolean;

  /**
   * Internal identifier of the player.
   */
  readonly playerId: number;

  /**
   * Name shown across the application.
   */
  readonly displayName: string;

  /**
   * Relative path or URL of the player portrait, or `null` when not yet synchronized.
   */
  readonly portrait: string | null;

  /**
   * Damage dealt by the day's valued matches, diminishing returns and streak bonus applied.
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
   * Matches priced below their full value by the day's diminishing returns.
   */
  readonly reducedMatchCount: number;

  /**
   * Consecutive active days.
   */
  readonly streakDays: number;

  /**
   * Bonus the streak grants, in percent.
   */
  readonly streakBonusPercent: number;

  /**
   * Streak the player loses by not playing today: the one ending yesterday. Zero once they
   * played, or when there was nothing to lose.
   */
  readonly streakAtStake: number;

  /**
   * Guardian damage before today.
   */
  readonly previousDamage: number;

  /**
   * {@link damage} minus {@link previousDamage} — what the day's board is really for.
   */
  readonly damageVariation: number;
}

/**
 * One day's ranking, priced on demand from the matches that day holds.
 *
 * Mirrors the backend `DailyRankingResponse` returned by `GET /api/rankings/daily`.
 */
export interface DailyRanking {
  /**
   * The day on the board, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly day: string;

  /**
   * The day the variation is measured against, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly previousDay: string;

  /**
   * Competing players who played at all that day.
   */
  readonly playedPlayerCount: number;

  /**
   * Competing players, the denominator {@link playedPlayerCount} is read against.
   */
  readonly rosterPlayerCount: number;

  /**
   * One entry per player of the roster, archived ones aside — deactivated players included, at a
   * `null` position.
   */
  readonly ranking: readonly DailyRankingEntry[];
}

/**
 * Single player's finalized result within one historical week.
 *
 * Mirrors `RankingHistoryWeekResponse.FinalRankingEntryResponse` from the backend.
 */
export interface RankingHistoryEntry {
  /**
   * Position on the board, first being 1.
   */
  readonly position: number;

  /**
   * Internal identifier of the player.
   */
  readonly playerId: number;

  /**
   * Name shown across the application.
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
   * The player's frozen weekly total, what the finalized position was ordered on.
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
   * Days with at least one match this week.
   */
  readonly activeDays: number;

  /**
   * Consecutive active days.
   */
  readonly streakDays: number;

  /**
   * Weekly titles held.
   */
  readonly titles: readonly WeeklyTitle[];
}

/**
 * Finalized, immutable ranking for one completed calendar week.
 *
 * Mirrors `RankingHistoryWeekResponse` from the backend.
 */
export interface RankingHistoryWeek {
  /**
   * Monday identifying the week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly weekStart: string;

  /**
   * Sunday identifying the week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly weekEnd: string;

  /**
   * Instant the week was frozen at, as an ISO-8601 instant.
   */
  readonly finalizedAt: string;

  /**
   * Who finished first, or `null` on a week nobody was ranked.
   */
  readonly winnerPlayerId: number | null;

  /**
   * Final ranking of the closed week.
   */
  readonly ranking: readonly RankingHistoryEntry[];
}

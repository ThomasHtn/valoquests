import { WeeklyTitle } from '@core/campaign/titles/campaign-title.model';
import { ChallengeCadence, ChallengeDifficulty } from '@core/challenges/challenge.model';
import { CompetitiveTier } from '@core/players/competitive-tier/player-competitive-tier.model';

/**
 * Player identity and rank of a ranking entry. Mirrors the backend `PlayerRankingResponse`.
 */
export interface PlayerRanking {
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
 * Progress on one board challenge, weekly or daily. Mirrors `ChallengeProgressResponse`.
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
   * Day of a daily (`YYYY-MM-DD`), `null` for a weekly.
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
   * Value to reach, `null` when open-ended.
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
   * Position at the previous calculation, `null` when new.
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
   * Days with at least one match this week.
   */
  readonly activeDays: number;

  /**
   * Days played in the week so far.
   */
  readonly streakDays: number;

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

  /**
   * One line per board challenge: the five weeklies and today's daily.
   */
  readonly challengeProgress: readonly RankingChallengeProgress[];
}

/**
 * Current weekly ranking with challenge progress. Mirrors the backend `CurrentRankingResponse`.
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
   * Last calculation instant (ISO-8601), `null` before the week's first one.
   */
  readonly calculatedAt: string | null;

  /**
   * Entries in board order.
   */
  readonly ranking: readonly RankingEntry[];
}

/**
 * One player's day against the day before. Mirrors the backend `DailyRankingEntryResponse`.
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
   * Week days played up to this day included, `0` when not played.
   */
  readonly streakDays: number;

  /**
   * Bonus the streak grants, in percent.
   */
  readonly streakBonusPercent: number;

  /**
   * Week days played up to this day included, ascending ISO dates.
   */
  readonly weekPlayedDays: readonly string[];

  /**
   * Guardian damage before today.
   */
  readonly previousDamage: number;

  /**
   * Damage minus previous damage, what the daily board is for.
   */
  readonly damageVariation: number;
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
   * Day the variation is measured against (`YYYY-MM-DD`).
   */
  readonly previousDay: string;

  /**
   * Competing players who played at all that day.
   */
  readonly playedPlayerCount: number;

  /**
   * Competing players, the denominator of `playedPlayerCount`.
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
   * Days with at least one match this week.
   */
  readonly activeDays: number;

  /**
   * Days played in the week.
   */
  readonly streakDays: number;

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
   * Freeze instant, ISO-8601.
   */
  readonly finalizedAt: string;

  /**
   * Winner, `null` when nobody was ranked.
   */
  readonly winnerPlayerId: number | null;

  /**
   * Final ranking of the closed week.
   */
  readonly ranking: readonly RankingHistoryEntry[];
}

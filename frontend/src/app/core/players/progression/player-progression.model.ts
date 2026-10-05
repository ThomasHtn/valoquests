import { MatchResult } from '@core/matches/match-result.model';
import { CompetitiveTier } from '../competitive-tier/player-competitive-tier.model';
import { WEEKDAY_NAMES } from './player-progression.constants';

/**
 * One match plotted on the evolution charts.
 */
interface ProgressionMatchPoint {
  /**
   * Start instant, as an ISO-8601 string.
   */
  readonly startedAt: string;

  /**
   * Share of hits that landed on the head, in percent.
   */
  readonly headshotPercentage: number;

  /**
   * Ratio of kills and assists to deaths.
   */
  readonly kda: number;

  /**
   * Average combat score, or `null` when Riot reported none.
   */
  readonly acs: number | null;

  /**
   * Average damage per round, or `null` when Riot reported none.
   */
  readonly adr: number | null;
}

/**
 * Season mean of each plotted metric, computed by the backend.
 */
interface ProgressionAverages {
  /**
   * Share of hits that landed on the head, in percent.
   */
  readonly headshotPercentage: number;

  /**
   * Ratio of kills and assists to deaths.
   */
  readonly kda: number;

  /**
   * Average combat score.
   */
  readonly acs: number;

  /**
   * Average damage per round.
   */
  readonly adr: number;
}

/**
 * One season's match-by-match progression.
 */
export interface SeasonEvolution {
  /**
   * Identifier of the season.
   */
  readonly seasonId: number;

  /**
   * Raw season code, e.g. `e9a2`.
   */
  readonly seasonName: string;

  /**
   * Whether this is the season in progress.
   */
  readonly active: boolean;

  /**
   * Matches of the season, oldest first.
   */
  readonly points: readonly ProgressionMatchPoint[];

  /**
   * Season averages the legend shows.
   */
  readonly averages: ProgressionAverages;
}

/**
 * Where registered hits land; shares of hits, not accuracy (Riot omits misses).
 */
export interface AimBreakdown {
  /**
   * Share of hits on the head, in percent.
   */
  readonly headPercentage: number;

  /**
   * Share of hits on the body, in percent.
   */
  readonly bodyPercentage: number;

  /**
   * Share of hits on the legs, in percent.
   */
  readonly legPercentage: number;

  /**
   * Hits counted.
   */
  readonly totalShots: number;
}

/**
 * One day of the week's performance. `day` is a Java `DayOfWeek` name, e.g. `MONDAY`.
 */
export interface WeekdayPerformance {
  /**
   * Day of the week.
   */
  readonly day: WeekdayName;

  /**
   * Matches played.
   */
  readonly matchesPlayed: number;

  /**
   * Matches won.
   */
  readonly wins: number;

  /**
   * Share of matches won, in percent.
   */
  readonly winRate: number;

  /**
   * Whether this is the best day with enough matches.
   */
  readonly best: boolean;
}

/**
 * Name of one day of the week.
 */
type WeekdayName = (typeof WEEKDAY_NAMES)[number];

/**
 * One three-hour slot's performance. `startHour` is that slot's first hour, 0 to 21.
 */
export interface HourSlotPerformance {
  /**
   * First hour of the slot, 0 to 21.
   */
  readonly startHour: number;

  /**
   * Matches played.
   */
  readonly matchesPlayed: number;

  /**
   * Matches won.
   */
  readonly wins: number;

  /**
   * Share of matches won, in percent.
   */
  readonly winRate: number;

  /**
   * Whether this is the best slot with enough matches.
   */
  readonly best: boolean;
}

/**
 * One personal best and the match it was set in.
 */
export interface RecordEntry {
  /**
   * Value of the record.
   */
  readonly value: number;

  /**
   * Start instant of the match the record was set in, ISO-8601.
   */
  readonly achievedAt: string;

  /**
   * Name of the map.
   */
  readonly mapName: string;

  /**
   * Agent played.
   */
  readonly agentName: string;
}

/**
 * A player's personal bests. Every per-match entry is `null` when no match qualified.
 */
export interface PersonalRecords {
  /**
   * Most kills in one match.
   */
  readonly mostKills: RecordEntry | null;

  /**
   * Best combat score in one match.
   */
  readonly bestAcs: RecordEntry | null;

  /**
   * Most damage in one match.
   */
  readonly mostDamage: RecordEntry | null;

  /**
   * Best KDA in one match.
   */
  readonly bestKda: RecordEntry | null;

  /**
   * Best headshot rate in one match long enough to count.
   */
  readonly bestHeadshotPercentage: RecordEntry | null;

  /**
   * Longest run of consecutive wins.
   */
  readonly longestWinStreak: number;

  /**
   * Longest run of consecutive days with at least one match.
   */
  readonly longestActiveDayStreak: number;

  /**
   * Matches finished as MVP.
   */
  readonly mvps: number;

  /**
   * Highest competitive rank held, or `null` when unranked throughout.
   */
  readonly peakTier: CompetitiveTier | null;
}

/**
 * Aggregated statistics for one map or one agent.
 */
interface ProgressionEntityStatistics {
  /**
   * Matches played.
   */
  readonly matchesPlayed: number;

  /**
   * Matches won.
   */
  readonly wins: number;

  /**
   * Loss examples at a few breakthrough levels.
   */
  readonly losses: number;

  /**
   * Share of matches won, in percent.
   */
  readonly winRate: number;

  /**
   * Ratio of kills and assists to deaths.
   */
  readonly kda: number;

  /**
   * Average damage per round.
   */
  readonly adr: number;

  /**
   * Average combat score.
   */
  readonly acs: number;
}

/**
 * Aggregated statistics for one map.
 */
interface MapStatistics extends ProgressionEntityStatistics {
  /**
   * Riot map identifier, or `null` when unknown.
   */
  readonly mapId: string | null;

  /**
   * Name of the map.
   */
  readonly mapName: string;
}

/**
 * Aggregated statistics for one agent.
 */
interface AgentStatistics extends ProgressionEntityStatistics {
  /**
   * Riot agent identifier, or `null` when unknown.
   */
  readonly agentId: string | null;

  /**
   * Agent played.
   */
  readonly agentName: string;
}

/**
 * Where one season left a player on the competitive ladder.
 */
export interface SeasonRank {
  /**
   * Identifier of the season.
   */
  readonly seasonId: number;

  /**
   * Raw season code, e.g. `e9a2`.
   */
  readonly seasonName: string;

  /**
   * Whether this is the season in progress.
   */
  readonly active: boolean;

  /**
   * Rank held after the season's last ranked match.
   */
  readonly finalTier: CompetitiveTier;

  /**
   * Highest rank held during the season.
   */
  readonly highestTier: CompetitiveTier;

  /**
   * Lowest rank held during the season, placements excluded.
   */
  readonly lowestTier: CompetitiveTier;

  /**
   * Competitive matches of the season, placements included.
   */
  readonly matchesPlayed: number;

  /**
   * Competitive matches of the season won.
   */
  readonly wins: number;

  /**
   * Rank held after each ranked match of the season, oldest first.
   */
  readonly rankedTiers: readonly CompetitiveTier[];
}

/**
 * One match plotted on the consistency chart.
 */
export interface ConsistencyMatch {
  /**
   * Start instant, as an ISO-8601 string.
   */
  readonly startedAt: string;

  /**
   * Average combat score of the match.
   */
  readonly acs: number;

  /**
   * Outcome for the player's team.
   */
  readonly result: MatchResult;

  /**
   * Rounds won by the player's team, or `null` when unreported.
   */
  readonly allyScore: number | null;

  /**
   * Rounds won by the opposing team, or `null` when unreported.
   */
  readonly enemyScore: number | null;

  /**
   * Name of the map.
   */
  readonly mapName: string;

  /**
   * Agent played.
   */
  readonly agentName: string;
}

/**
 * Combat-score steadiness: the middle half of matches between a floor and a ceiling.
 */
export interface ConsistencySummary {
  /**
   * First quartile of the combat scores.
   */
  readonly floor: number;

  /**
   * Median combat score.
   */
  readonly median: number;

  /**
   * Third quartile of the combat scores.
   */
  readonly ceiling: number;

  /**
   * Gap between ceiling and floor.
   */
  readonly spread: number;

  /**
   * Seasons the matches come from.
   */
  readonly seasonCount: number;

  /**
   * Previous season with enough matches, for a single-season selection, else `null`.
   */
  readonly previousSeasonName: string | null;

  /**
   * That previous season's spread, or `null`.
   */
  readonly previousSpread: number | null;

  /**
   * Every match with a combat score, oldest first.
   */
  readonly matches: readonly ConsistencyMatch[];
}

/**
 * Progression view of one player and season selection.
 * Competitive matches only, except `records.longestActiveDayStreak` which counts any mode.
 */
export interface PlayerProgression {
  /**
   * Match-by-match progression per selected season.
   */
  readonly evolution: readonly SeasonEvolution[];

  /**
   * Where the player’s hits land.
   */
  readonly aim: AimBreakdown;

  /**
   * Performance per day of the week.
   */
  readonly weekdays: readonly WeekdayPerformance[];

  /**
   * Performance per three-hour slot.
   */
  readonly hourSlots: readonly HourSlotPerformance[];

  /**
   * Personal bests.
   */
  readonly records: PersonalRecords;

  /**
   * Per-map statistics, most played first.
   */
  readonly maps: readonly MapStatistics[];

  /**
   * Per-agent statistics, most played first.
   */
  readonly agents: readonly AgentStatistics[];

  /**
   * Where every selected season ended on the ladder, oldest first.
   */
  readonly rankJourney: readonly SeasonRank[];

  /**
   * Combat-score spread over the selection, or `null` when it holds too few matches.
   */
  readonly consistency: ConsistencySummary | null;
}

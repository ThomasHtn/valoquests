import { CompetitiveTier } from './competitive-tier.model';

/**
 * One match plotted on the evolution charts.
 */
export interface ProgressionMatchPoint {
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
 * A season's mean value for each plotted metric, as the backend computed it.
 *
 * Read straight into the chart legend: this application renders figures, it never derives them.
 */
export interface ProgressionAverages {
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
 * Where a player's registered hits land.
 *
 * A share of hits, not an accuracy: Riot reports nothing about the shots that missed entirely.
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
 * Days of the week, in the order the backend returns them.
 */
export const WEEKDAY_NAMES = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
] as const;

/**
 * Name of one day of the week.
 */
export type WeekdayName = (typeof WEEKDAY_NAMES)[number];

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
export interface ProgressionEntityStatistics {
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
export interface MapStatistics extends ProgressionEntityStatistics {
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
export interface AgentStatistics extends ProgressionEntityStatistics {
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
 * Everything the profile's progression view renders, for one player and one season selection.
 *
 * Every figure is scoped to competitive matches — the only queue whose combat score, damage per
 * round and win rate compare across matches — except `records.longestActiveDayStreak`, which
 * counts showing up in any mode.
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
}

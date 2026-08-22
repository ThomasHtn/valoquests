import { CompetitiveTier } from './competitive-tier.model';

/**
 * One match plotted on the evolution charts.
 */
export interface ProgressionMatchPoint {
  readonly startedAt: string;
  readonly headshotPercentage: number;
  readonly kda: number;
  readonly acs: number | null;
  readonly adr: number | null;
}

/**
 * A season's mean value for each plotted metric, as the backend computed it.
 *
 * Read straight into the chart legend: this application renders figures, it never derives them.
 */
export interface ProgressionAverages {
  readonly headshotPercentage: number;
  readonly kda: number;
  readonly acs: number;
  readonly adr: number;
}

/**
 * One season's match-by-match progression.
 */
export interface SeasonEvolution {
  readonly seasonId: number;
  readonly seasonName: string;
  readonly active: boolean;
  readonly points: readonly ProgressionMatchPoint[];
  readonly averages: ProgressionAverages;
}

/**
 * Where a player's registered hits land.
 *
 * A share of hits, not an accuracy: Riot reports nothing about the shots that missed entirely.
 */
export interface AimBreakdown {
  readonly headPercentage: number;
  readonly bodyPercentage: number;
  readonly legPercentage: number;
  readonly totalShots: number;
}

/**
 * One day of the week's performance. `day` is a Java `DayOfWeek` name, e.g. `MONDAY`.
 */
export interface WeekdayPerformance {
  readonly day: WeekdayName;
  readonly matchesPlayed: number;
  readonly wins: number;
  readonly winRate: number;
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
  readonly startHour: number;
  readonly matchesPlayed: number;
  readonly wins: number;
  readonly winRate: number;
  readonly best: boolean;
}

/**
 * One personal best and the match it was set in.
 */
export interface RecordEntry {
  readonly value: number;
  readonly achievedAt: string;
  readonly mapName: string;
  readonly agentName: string;
}

/**
 * A player's personal bests. Every per-match entry is `null` when no match qualified.
 */
export interface PersonalRecords {
  readonly mostKills: RecordEntry | null;
  readonly bestAcs: RecordEntry | null;
  readonly mostDamage: RecordEntry | null;
  readonly bestKda: RecordEntry | null;
  readonly bestHeadshotPercentage: RecordEntry | null;
  readonly longestWinStreak: number;
  readonly longestActiveDayStreak: number;
  readonly mvps: number;
  readonly peakTier: CompetitiveTier | null;
}

/**
 * Aggregated statistics for one map or one agent.
 */
export interface ProgressionEntityStatistics {
  readonly matchesPlayed: number;
  readonly wins: number;
  readonly losses: number;
  readonly winRate: number;
  readonly kda: number;
  readonly adr: number;
  readonly acs: number;
}

/**
 * Aggregated statistics for one map.
 */
export interface MapStatistics extends ProgressionEntityStatistics {
  readonly mapId: string | null;
  readonly mapName: string;
}

/**
 * Aggregated statistics for one agent.
 */
export interface AgentStatistics extends ProgressionEntityStatistics {
  readonly agentId: string | null;
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
  readonly evolution: readonly SeasonEvolution[];
  readonly aim: AimBreakdown;
  readonly weekdays: readonly WeekdayPerformance[];
  readonly hourSlots: readonly HourSlotPerformance[];
  readonly records: PersonalRecords;
  readonly maps: readonly MapStatistics[];
  readonly agents: readonly AgentStatistics[];
}

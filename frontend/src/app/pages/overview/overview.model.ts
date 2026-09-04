import { ExtractionLimiter, GuardianCategory, WeeklyTitle } from '@core/campaign/campaign.model';
import { TitleVisual } from '@core/campaign/campaign-visual.utils';

/**
 * One of the ten weeks on the frieze: its issue, and how far the guardian was pushed.
 */
export interface FriezeWeek {
  readonly index: number;
  readonly label: string;
  readonly state: 'won' | 'lost' | 'now' | 'ahead';

  /**
   * Share of the guardian's hit points taken, in [0, 1], carried by the top rule.
   */
  readonly advance: number;
  readonly mark: string;
  readonly title: string;
}

/**
 * The week in progress, as the situation report states it.
 */
export interface Mission {
  readonly weekIndex: number;
  readonly planetName: string;
  readonly category: GuardianCategory;
  readonly dayOfWeek: number;
  readonly guardianName: string;
  readonly hitPointsLeft: number;
  readonly hitPoints: number;
  readonly breachPercent: number;

  /**
   * Share of the hit points still standing, in [0, 1], what the health bar and the ring show.
   */
  readonly guardianLeft: number;
  readonly wounded: number;
  readonly crew: number;

  /**
   * Instant the week ends at, in epoch milliseconds.
   */
  readonly extractionDeadline: number;
}

/**
 * One dial of the extraction capacity: a figure over the wounded spotted, and the raw stock
 * that produces it.
 */
export interface Gauge {
  readonly value: number;
  readonly fraction: number;
  readonly stock: number;
}

/**
 * The four dials: three limits, then what gets through.
 */
export interface Capacity {
  readonly wounded: number;
  readonly carry: Gauge;
  readonly shelter: Gauge;
  readonly breach: Gauge;
  readonly aboard: number;
  readonly aboardFraction: number;
  readonly fromGuardian: number;
  readonly fromChallenges: number;
  readonly leftBehind: number;
  readonly limiter: ExtractionLimiter;
  readonly componentsPerRescue: number;
  readonly foodPerRescue: number;

  /**
   * Hit points one percent of breakthrough costs.
   */
  readonly hitPointsPerPercent: number;
}

/**
 * The day's challenge and who has validated it.
 */
export interface DailyOrder {
  readonly name: string;
  readonly description: string;
  readonly survivors: number;
  readonly validated: readonly { readonly name: string; readonly done: boolean }[];
  readonly doneCount: number;

  /**
   * Midnight tonight, in epoch milliseconds.
   */
  readonly deadline: number;
}

/**
 * What the day has given, line by line.
 */
export interface DayTally {
  readonly guardianName: string;
  readonly damage: number;
  readonly components: number;
  readonly carryGained: number;
  readonly food: number;
  readonly shelterGained: number;
  readonly upkeep: number;
  readonly population: number;
  readonly presence: number;
  readonly roster: number;

  /**
   * One slot per operator of the roster, lit for those who played.
   */
  readonly pips: readonly boolean[];
}

/**
 * One operator's day on the squad sheet.
 */
export interface SquadRow {
  readonly position: number | null;
  readonly playerId: number;
  readonly name: string;
  readonly portrait: string | null;
  readonly title: (TitleVisual & { readonly key: WeeklyTitle }) | null;
  readonly played: boolean;
  readonly streakMultiplier: string;
  readonly streakDays: number;
  readonly streakAtStake: number;
  readonly damage: number;
  readonly matchCount: number;
  readonly reducedMatchCount: number;
  readonly components: number;
  readonly food: number;
}

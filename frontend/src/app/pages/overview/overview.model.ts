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
 * One of the four weekly titles as the mission report lists it: its holder, or nobody.
 */
export interface MissionReportTitle extends TitleVisual {
  readonly key: WeeklyTitle;
  readonly holder: string | null;
  readonly portrait: string | null;
}

/**
 * One line of the frozen ranking the mission report shows.
 */
export interface MissionReportRank {
  readonly position: number;
  readonly name: string;
  readonly portrait: string | null;
  readonly total: number;
}

/**
 * The last settled week, as the Monday report tells it: the guardian, the wounded, the base, the
 * titles, the frozen ranking, and the planet ahead.
 */
export interface MissionReport {
  readonly weekStart: string;
  readonly weekIndex: number;
  readonly planetName: string;
  readonly settledOn: string;
  readonly guardianName: string;
  readonly defeated: boolean;
  readonly hitPoints: number;
  readonly hitPointsLeft: number;
  readonly breachPercent: number;

  /**
   * The fatal blow in one line (who, when, where), or `null` when the guardian held.
   */
  readonly blow: string | null;
  readonly baseLoss: number;
  readonly rescued: number;
  readonly spotted: number;
  readonly byChallenges: number;
  readonly limiter: ExtractionLimiter;

  /**
   * Inhabitants at the week's close, `null` when no day of it was replayed.
   */
  readonly population: number | null;
  readonly populationChange: number;

  /**
   * The four titles, or `null` when the week's ranking was never frozen.
   */
  readonly titles: readonly MissionReportTitle[] | null;
  readonly ranking: readonly MissionReportRank[];
  readonly next: {
    readonly planetName: string;
    readonly hitPoints: number;
    readonly wounded: number;
  } | null;
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

  /**
   * The fatal blow, once the guardian is down: weekday and time of the match, and who played it
   * (`null` while the roster is not known yet). `null` while the guardian stands.
   */
  readonly defeated: {
    readonly weekday: string;
    readonly time: string;
    readonly by: string | null;
  } | null;
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

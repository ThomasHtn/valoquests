import { ExtractionLimiter, GuardianCategory, WeeklyTitle } from '@core/campaign/campaign.model';
import { TitleVisual } from '@core/campaign/campaign-visual.utils';

/**
 * One of the ten weeks on the frieze: its issue, and how far the guardian was pushed.
 */
export interface FriezeWeek {
  /**
   * Week index, one-based.
   */
  readonly index: number;

  /**
   * Two-digit week index.
   */
  readonly label: string;

  /**
   * Won, lost, in progress, or ahead.
   */
  readonly state: 'won' | 'lost' | 'now' | 'ahead';

  /**
   * Share of the guardian's hit points taken, in [0, 1], carried by the top rule.
   */
  readonly advance: number;

  /**
   * Short mark drawn in the cell.
   */
  readonly mark: string;

  /**
   * Tooltip of the cell.
   */
  readonly title: string;
}

/**
 * One of the four weekly titles as the mission report lists it: its holder, or nobody.
 */
export interface MissionReportTitle extends TitleVisual {
  /**
   * Which weekly title.
   */
  readonly key: WeeklyTitle;

  /**
   * Name of the holder, or `null` when nobody earned it.
   */
  readonly holder: string | null;

  /**
   * Bundled agent portrait name, or `null` when none was chosen.
   */
  readonly portrait: string | null;
}

/**
 * One line of the frozen ranking the mission report shows.
 */
export interface MissionReportRank {
  /**
   * Position on the board, first being 1.
   */
  readonly position: number;

  /**
   * Operator name.
   */
  readonly name: string;

  /**
   * Bundled agent portrait name, or `null` when none was chosen.
   */
  readonly portrait: string | null;

  /**
   * Total ranking points.
   */
  readonly total: number;
}

/**
 * The last settled week, as the Monday report tells it: the guardian, the wounded, the base, the
 * titles, the frozen ranking, and the planet ahead.
 */
export interface MissionReport {
  /**
   * Monday of the week, ISO date.
   */
  readonly weekStart: string;

  /**
   * One-based index of the week in the campaign.
   */
  readonly weekIndex: number;

  /**
   * Name of the planet.
   */
  readonly planetName: string;

  /**
   * Date the week was settled, ISO.
   */
  readonly settledOn: string;

  /**
   * Name of the guardian.
   */
  readonly guardianName: string;

  /**
   * Whether the guardian was defeated.
   */
  readonly defeated: boolean;

  /**
   * Hit points the guardian started with.
   */
  readonly hitPoints: number;

  /**
   * Hit points the guardian still has.
   */
  readonly hitPointsLeft: number;

  /**
   * Share of the guardian’s hit points taken, in percent.
   */
  readonly breachPercent: number;

  /**
   * The fatal blow in one line (who, when, where), or `null` when the guardian held.
   */
  readonly blow: string | null;

  /**
   * Inhabitants lost to the guardian left standing.
   */
  readonly baseLoss: number;

  /**
   * Wounded brought home.
   */
  readonly rescued: number;

  /**
   * Wounded spotted on the planet.
   */
  readonly spotted: number;

  /**
   * Wounded rescued through validated challenges.
   */
  readonly byChallenges: number;

  /**
   * What capped the extraction: a stock, the group, or nothing.
   */
  readonly limiter: ExtractionLimiter;

  /**
   * Inhabitants at the week's close, `null` when no day of it was replayed.
   */
  readonly population: number | null;

  /**
   * Change of population over the period.
   */
  readonly populationChange: number;

  /**
   * The four titles, or `null` when the week's ranking was never frozen.
   */
  readonly titles: readonly MissionReportTitle[] | null;

  /**
   * Frozen ranking of the week.
   */
  readonly ranking: readonly MissionReportRank[];

  /**
   * The planet ahead.
   */
  readonly next: {
    /**
     * Name of the planet.
     */
    readonly planetName: string;

    /**
     * Hit points the guardian started with.
     */
    readonly hitPoints: number;

    /**
     * Wounded waiting on the planet.
     */
    readonly wounded: number;
  } | null;
}

/**
 * The week in progress, as the situation report states it.
 */
export interface Mission {
  /**
   * One-based index of the week in the campaign.
   */
  readonly weekIndex: number;

  /**
   * Name of the planet.
   */
  readonly planetName: string;

  /**
   * Weight class of the guardian.
   */
  readonly category: GuardianCategory;

  /**
   * Day of the week, Monday being 1.
   */
  readonly dayOfWeek: number;

  /**
   * Name of the guardian.
   */
  readonly guardianName: string;

  /**
   * Hit points the guardian still has.
   */
  readonly hitPointsLeft: number;

  /**
   * Hit points the guardian started with.
   */
  readonly hitPoints: number;

  /**
   * Share of the guardian’s hit points taken, in percent.
   */
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
    /**
     * Weekday of the last event, formatted.
     */
    readonly weekday: string;

    /**
     * Time of the last event, formatted.
     */
    readonly time: string;

    /**
     * Operator behind the last event, or `null`.
     */
    readonly by: string | null;
  } | null;

  /**
   * Wounded waiting on the planet.
   */
  readonly wounded: number;

  /**
   * Operators on the roster.
   */
  readonly crew: number;

  /**
   * Instant the week ends at, in epoch milliseconds.
   */
  readonly extractionDeadline: number;
}

/**
 * One operator's share of the squad's weekly contribution, as one segment of the contribution bar.
 */
export interface ContributionShare {
  /**
   * Internal identifier of the player.
   */
  readonly playerId: number;

  /**
   * Operator name.
   */
  readonly name: string;

  /**
   * Damage dealt to the week's guardian, streak bonus included.
   */
  readonly damage: number;

  /**
   * Ranking points banked by the challenges validated this week.
   */
  readonly challengePoints: number;

  /**
   * {@link damage} + {@link challengePoints}: what the ranking orders on.
   */
  readonly total: number;

  /**
   * Share of the guardian's hit points this segment covers, in [0, 1] — the segment's width.
   */
  readonly fraction: number;

  /**
   * Share of the squad's own contribution, in percent — what the segment is worth beside the others.
   */
  readonly sharePercent: number;
}

/**
 * What the squad has put into the week, read on the guardian's own scale: the whole bar is the
 * guardian's hit points, each segment one operator, and the empty end what is left to do.
 */
export interface Contribution {
  /**
   * Damage dealt by the squad this week.
   */
  readonly total: number;

  /**
   * Hit points the guardian started with.
   */
  readonly hitPoints: number;

  /**
   * One segment per operator.
   */
  readonly shares: readonly ContributionShare[];
}

/**
 * One dial of the extraction capacity: a figure over the wounded spotted, and the raw stock
 * that produces it.
 */
export interface Gauge {
  /**
   * Wounded the dial allows.
   */
  readonly value: number;

  /**
   * Fill of the dial, in [0, 1].
   */
  readonly fraction: number;

  /**
   * Units behind the dial.
   */
  readonly stock: number;
}

/**
 * The four dials: three limits, then what gets through.
 */
export interface Capacity {
  /**
   * Wounded waiting on the planet.
   */
  readonly wounded: number;

  /**
   * Wounded the components can carry.
   */
  readonly carry: Gauge;

  /**
   * Wounded the food can shelter.
   */
  readonly shelter: Gauge;

  /**
   * Breakthrough dial.
   */
  readonly breach: Gauge;

  /**
   * Wounded aboard tonight.
   */
  readonly aboard: number;

  /**
   * Aboard as a share of the wounded, in [0, 1].
   */
  readonly aboardFraction: number;

  /**
   * Wounded freed by the breakthrough.
   */
  readonly fromGuardian: number;

  /**
   * Wounded freed by challenges.
   */
  readonly fromChallenges: number;

  /**
   * Wounded left on the planet.
   */
  readonly leftBehind: number;

  /**
   * What capped the extraction: a stock, the group, or nothing.
   */
  readonly limiter: ExtractionLimiter;

  /**
   * Components one rescue costs.
   */
  readonly componentsPerRescue: number;

  /**
   * Food one rescue costs.
   */
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
  /**
   * Translated name of the challenge.
   */
  readonly name: string;

  /**
   * What has to be done, translated.
   */
  readonly description: string;

  /**
   * Survivors the challenge is worth.
   */
  readonly survivors: number;

  /**
   * One entry per operator, lit when validated.
   */
  readonly validated: readonly { readonly name: string; readonly done: boolean }[];

  /**
   * Operators who validated it.
   */
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
  /**
   * Name of the guardian.
   */
  readonly guardianName: string;

  /**
   * Guardian damage dealt today.
   */
  readonly damage: number;

  /**
   * Components gained today.
   */
  readonly components: number;

  /**
   * Carry capacity gained today.
   */
  readonly carryGained: number;

  /**
   * Food gained today.
   */
  readonly food: number;

  /**
   * Shelter capacity gained today.
   */
  readonly shelterGained: number;

  /**
   * Food eaten today.
   */
  readonly upkeep: number;

  /**
   * Inhabitants tonight.
   */
  readonly population: number;

  /**
   * Operators who played today.
   */
  readonly presence: number;

  /**
   * Operators on the roster.
   */
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
  /**
   * Position on the weekly board, or `null` when unranked.
   */
  readonly position: number | null;

  /**
   * Internal identifier of the player.
   */
  readonly playerId: number;

  /**
   * Operator name.
   */
  readonly name: string;

  /**
   * Bundled agent portrait name, or `null` when none was chosen.
   */
  readonly portrait: string | null;

  /**
   * Weekly title held, with its visual, or `null`.
   */
  readonly title: (TitleVisual & { readonly key: WeeklyTitle }) | null;

  /**
   * Whether the operator played today.
   */
  readonly played: boolean;

  /**
   * Streak multiplier, formatted.
   */
  readonly streakMultiplier: string;

  /**
   * Consecutive active days.
   */
  readonly streakDays: number;

  /**
   * Streak days lost if the operator sits out today.
   */
  readonly streakAtStake: number;

  /**
   * Guardian damage dealt today.
   */
  readonly damage: number;

  /**
   * Matches played.
   */
  readonly matchCount: number;

  /**
   * Matches paid below full value.
   */
  readonly reducedMatchCount: number;

  /**
   * Components gained today.
   */
  readonly components: number;

  /**
   * Food gained today.
   */
  readonly food: number;
}

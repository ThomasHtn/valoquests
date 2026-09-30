import { ExtractionLimiter, GuardianCategory, WeeklyTitle } from '@core/campaign/campaign.model';
import { TitleVisual } from '@core/campaign/campaign-visual.utils';
import { StreakPip } from '@shared/streak-gauge/streak-gauge.model';

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
   * Name of the planet.
   */
  readonly name: string;

  /**
   * Public path of the planet's drawing.
   */
  readonly art: string;

  /**
   * Won, lost, in progress, or ahead.
   */
  readonly state: 'won' | 'lost' | 'now' | 'ahead';

  /**
   * Whether Sunday has settled the week, so its mission report can be opened from the cell.
   */
  readonly settled: boolean;

  /**
   * Share of the guardian's hit points still standing, in [0, 1], carried by the ring around the
   * planet: full before the fight, empty once the guardian is down.
   */
  readonly standing: number;

  /**
   * Abbreviated level of the week's guardian, short enough to share a line with the status.
   */
  readonly level: string;

  /**
   * Short status beside the level: defeated, survived with its hit points left, the breakthrough,
   * or what is ahead.
   */
  readonly status: string;

  /**
   * What screen readers hear for the cell: the guardian's level in full, then how the week went.
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
 * The fatal blow as the mission report tells it, split so the player's name can stand out.
 */
export interface MissionReportBlow {
  /**
   * When the guardian fell, as the sentence opens (`Guardian defeated on Sunday at 15:04`).
   */
  readonly when: string;

  /**
   * Who dealt the blow, or `null` when the player is unknown.
   */
  readonly by: string | null;

  /**
   * Map, mode and score of the match, with their leading punctuation, or empty when unknown.
   */
  readonly where: string;
}

/**
 * The week's champion as the mission report crowns it, beside the four titles: its holder, or nobody.
 */
export interface MissionReportChampion {
  /**
   * Name of the champion, or `null` on a shared first place.
   */
  readonly holder: string | null;

  /**
   * Bundled agent portrait name, or `null` when none was chosen.
   */
  readonly portrait: string | null;

  /**
   * The champion's ranking points, `0` when nobody won outright.
   */
  readonly points: number;
}

/**
 * The last settled week, as the Monday report tells it: the verdict, the loot brought home, the
 * honours, and the planet ahead.
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
   * Whether the guardian was defeated.
   */
  readonly defeated: boolean;

  /**
   * Share of the guardian’s hit points taken, in percent.
   */
  readonly breachPercent: number;

  /**
   * The fatal blow, or `null` when the guardian held.
   */
  readonly blow: MissionReportBlow | null;

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
   * Share of the spotted wounded brought home, from 0 to 1.
   */
  readonly rescuedShare: number;

  /**
   * Wounded rescued through validated challenges.
   */
  readonly byChallenges: number;

  /**
   * Wounded extracted by the ship.
   */
  readonly byShip: number;

  /**
   * Wounded left on the planet.
   */
  readonly leftBehind: number;

  /**
   * What capped the extraction: a stock, the group, or nothing.
   */
  readonly limiter: ExtractionLimiter;

  /**
   * What the week harvested and how the base ended it, `null` when no day of it was replayed.
   */
  readonly base: {
    /**
     * Food gained over the week.
     */
    readonly foodGained: number;

    /**
     * Components gained over the week.
     */
    readonly componentsGained: number;

    /**
     * Inhabitants at the week's close.
     */
    readonly population: number;

    /**
     * Change of population over the week.
     */
    readonly populationChange: number;
  } | null;

  /**
   * The four titles, or `null` when the week's ranking was never frozen.
   */
  readonly titles: readonly MissionReportTitle[] | null;

  /**
   * The week's champion, or `null` when the week's ranking was never frozen.
   */
  readonly champion: MissionReportChampion | null;

  /**
   * The planet ahead, `null` after the last week.
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
   * Instant the week ends at (Monday 00:00 in the campaign time zone), in epoch milliseconds.
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
 * What Sunday midnight can still change, measured against the current forecast.
 */
export interface SundayStakes {
  /**
   * Wounded the ship would bring home on top of the forecast if the guardian fell.
   */
  readonly gain: number;

  /**
   * Inhabitants the guardian would kill if it held at the current breakthrough.
   */
  readonly loss: number;
}

/**
 * What the day has given, line by line.
 */
export interface DayTally {
  /**
   * One-based index of the week in the campaign, worded into the label as `Boss 04` rather than
   * the guardian's own name.
   */
  readonly weekIndex: number;

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
   * Inhabitants gained or lost today, damage growth net of famine and guardian losses.
   */
  readonly populationChange: number;

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
  readonly pips: readonly DayPip[];
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
   * Whether the operator holds the reigning Champion title.
   */
  readonly champion: boolean;

  /**
   * Weekly title held, with its visual, or `null`.
   */
  readonly title: (TitleVisual & { readonly key: WeeklyTitle }) | null;

  /**
   * Whether the operator played today.
   */
  readonly played: boolean;

  /**
   * Streak bonus of today's matches in percent, the one playing would earn while not played yet.
   */
  readonly streakBonusPercent: number;

  /**
   * Days of the week played so far, today included once played.
   */
  readonly streakDays: number;

  /**
   * The week from Monday to Sunday, one pip per day.
   */
  readonly streakWeek: readonly StreakPip[];

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

/**
 * One operator slot of the day's presence gauge.
 */
export interface DayPip {
  /**
   * Operator who played, or `null` for an empty slot: absentees are not named by the day.
   */
  readonly name: string | null;

  /**
   * Portrait of the operator who played, or `null` for an empty slot or a player without one.
   */
  readonly portrait: string | null;

  /**
   * Whether the slot is lit.
   */
  readonly on: boolean;
}

/**
 * One tab of the overview's tab bar, in bar order.
 */
export type OverviewTabKey = 'challenges' | 'contributions' | 'matches' | 'campaign';

/**
 * One tab as the bar draws it.
 */
export interface OverviewTab {
  /**
   * Which tab.
   */
  readonly key: OverviewTabKey;

  /**
   * Translated name of the tab.
   */
  readonly label: string;
}

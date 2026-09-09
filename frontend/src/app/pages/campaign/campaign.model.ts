import {
  CampaignDifficulty,
  CampaignWeek,
  ExtractionLimiter,
  GuardianCategory,
} from '@core/campaign/campaign.model';
import { ChartSeries } from '@shared/chart/chart.model';

/**
 * Where one of the ten planets stands on the road.
 */
export type PlanetState = 'won' | 'lost' | 'now' | 'ahead';

/**
 * Icon a planet's state line carries, matched against a `@switch` at the call site.
 */
export type PlanetStateIcon = 'check' | 'x' | 'swords' | 'star' | null;

/**
 * Report of the week in progress.
 */
export interface NowReport {
  /**
   * Discriminant: the week in progress.
   */
  readonly kind: 'now';

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
   * Whole days left before Sunday's extraction, zero on Sunday itself.
   */
  readonly daysLeft: number;

  /**
   * Wounded waiting on the planet.
   */
  readonly wounded: number;

  /**
   * Wounded the extraction would bring home if it left tonight.
   */
  readonly tonight: number;
}

/**
 * Report of a settled week.
 */
export interface SettledReport {
  /**
   * Discriminant: a settled week.
   */
  readonly kind: 'settled';

  /**
   * Whether the guardian was defeated.
   */
  readonly defeated: boolean;

  /**
   * Weekday the guardian fell on, empty when it held.
   */
  readonly defeatedWeekday: string;

  /**
   * Who played the fatal blow, `null` when the guardian held or the roster is not loaded.
   */
  readonly defeatedBy: string | null;

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
   * Inhabitants lost to the guardian left standing.
   */
  readonly baseLoss: number;
}

/**
 * What can be said of a week still ahead.
 */
export interface AheadReport {
  /**
   * Discriminant: a week still ahead.
   */
  readonly kind: 'ahead';

  /**
   * Whether this is the tenth and last planet.
   */
  readonly final: boolean;
  /**
   * True on a closed campaign: the planet was never reached.
   */
  readonly unplayed: boolean;
}

/**
 * Report shown in the drawer, discriminated by `kind`.
 */
export type PlanetReport = NowReport | SettledReport | AheadReport;

/**
 * One of the ten planets on the strip, and its report in the drawer.
 */
export interface Planet {
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
   * Weight class of the guardian.
   */
  readonly category: GuardianCategory;

  /**
   * Current state.
   */
  readonly state: PlanetState;

  /**
   * Whether this is the tenth and last planet.
   */
  readonly final: boolean;

  /**
   * Share of the guardian's hit points taken, in [0, 1].
   */
  readonly advance: number;

  /**
   * Radius of the orb, in viewBox units.
   */
  readonly radius: number;

  /**
   * Ground colour of the orb.
   */
  readonly hue: string;

  /**
   * Translated line describing the state.
   */
  readonly stateLabel: string;

  /**
   * Icon of the state line.
   */
  readonly stateIcon: PlanetStateIcon;

  /**
   * Report shown in the drawer.
   */
  readonly report: PlanetReport;
}

/**
 * The rule of Sunday's settlement, with the figures of the week in progress in every term.
 */
export interface RescueLaw {
  /**
   * Wounded the components can carry.
   */
  readonly carry: number;

  /**
   * Wounded the food can shelter.
   */
  readonly shelter: number;

  /**
   * Wounded waiting on the planet.
   */
  readonly wounded: number;

  /**
   * Name of the planet.
   */
  readonly planetName: string;

  /**
   * Share of the guardian’s hit points taken, in percent.
   */
  readonly breachPercent: number;

  /**
   * Wounded the extraction brings home.
   */
  readonly extracted: number;

  /**
   * Wounded rescued through validated challenges.
   */
  readonly byChallenges: number;

  /**
   * Components one rescue costs.
   */
  readonly componentsPerRescue: number;

  /**
   * Food one rescue costs.
   */
  readonly foodPerRescue: number;

  /**
   * Hit points behind one percent of breakthrough.
   */
  readonly hitPointsPerPercent: number;
}

/**
 * What a guardian left standing would cost the base as it stands, at three breakthroughs.
 */
export interface LossExample {
  /**
   * Share of the guardian’s hit points taken, in percent.
   */
  readonly breachPercent: number;

  /**
   * Inhabitants lost.
   */
  readonly lost: number;
}

/**
 * The three notes under the formula.
 */
export interface LawNotes {
  /**
   * Tier the squad was measured at.
   */
  readonly difficulty: CampaignDifficulty;

  /**
   * Reference figure the squad was calibrated on.
   */
  readonly reference: number;

  /**
   * Inhabitants of the base.
   */
  readonly population: number;
  /**
   * True when `population` is a sample figure, the base being empty.
   */
  readonly sample: boolean;

  /**
   * Loss examples at a few breakthrough levels.
   */
  readonly losses: readonly LossExample[];
}

/**
 * One stock of the base and the wounded it can pay for.
 */
export interface Tank {
  /**
   * Units in stock.
   */
  readonly stock: number;

  /**
   * Wounded the stock can pay for.
   */
  readonly capacity: number;

  /**
   * Capacity over the wounded spotted, in [0, 1].
   */
  readonly fraction: number;
}

/**
 * The base's reserves and the tally of the rescue since the campaign opened.
 */
export interface Reserves {
  /**
   * Food.
   */
  readonly food: Tank;

  /**
   * Components.
   */
  readonly components: Tank;

  /**
   * Food the base eats per day.
   */
  readonly dailyUpkeep: number;

  /**
   * Wounded waiting on the planet.
   */
  readonly wounded: number;

  /**
   * Name of the planet.
   */
  readonly planetName: string;

  /**
   * Wounded brought home.
   */
  readonly rescued: number;

  /**
   * Wounded spotted on the planet.
   */
  readonly spotted: number;

  /**
   * Wounded rescued by the Sunday extraction.
   */
  readonly byExtraction: number;

  /**
   * Wounded rescued through validated challenges.
   */
  readonly byChallenges: number;

  /**
   * Share of rescues owed to challenges, in percent.
   */
  readonly byChallengesPercent: number;

  /**
   * Wounded left on the planet.
   */
  readonly leftBehind: number;

  /**
   * The three shares of the stacked bar, in percent of the spotted.
   */
  readonly shares: readonly [number, number, number];

  /**
   * Guardians defeated so far.
   */
  readonly guardiansDefeated: number;

  /**
   * Weeks already settled.
   */
  readonly weeksSettled: number;

  /**
   * Weeks capped by food.
   */
  readonly limitedByFood: number;

  /**
   * Weeks capped by components.
   */
  readonly limitedByComponents: number;

  /**
   * Weeks where the whole group got through.
   */
  readonly wholeGroup: number;
}

/**
 * One column head of the ledger: a planet.
 */
export interface LedgerColumn {
  /**
   * Week index, one-based.
   */
  readonly index: number;

  /**
   * Name of the planet.
   */
  readonly name: string;

  /**
   * Current state.
   */
  readonly state: PlanetState;
}

/**
 * One week of one resource in the ledger.
 */
export interface LedgerCell {
  /**
   * Week index, one-based.
   */
  readonly index: number;

  /**
   * Name of the planet.
   */
  readonly planetName: string;

  /**
   * Whether the week is settled, in progress, or ahead.
   */
  readonly kind: 'settled' | 'now' | 'ahead';

  /**
   * Units gained during the week.
   */
  readonly got: number;

  /**
   * Units spent on the Sunday extraction.
   */
  readonly spent: number;

  /**
   * Stock carried into the next week.
   */
  readonly carry: number;

  /**
   * Units carried in from the previous week.
   */
  readonly carriedIn: number;

  /**
   * Stock at the end of the week's last replayed day.
   */
  readonly stock: number;

  /**
   * Rescues the week’s gain pays for.
   */
  readonly rescues: number;

  /**
   * Rescues the stock pays for.
   */
  readonly stockRescues: number;

  /**
   * The three bars, as shares of the ledger's scale in [0, 1].
   */
  readonly gotShare: number;

  /**
   * Spent units as a share of the tallest bar.
   */
  readonly spentShare: number;

  /**
   * Carried units as a share of the tallest bar.
   */
  readonly carryShare: number;
}

/**
 * One row of the ledger: a resource across the ten weeks.
 */
export interface LedgerRow {
  /**
   * Resource of the row.
   */
  readonly key: 'food' | 'components';

  /**
   * Units gained over the campaign.
   */
  readonly gained: number;

  /**
   * Units spent over the campaign.
   */
  readonly spent: number;

  /**
   * One cell per planet.
   */
  readonly cells: readonly LedgerCell[];

  /**
   * Bar heights of the folded miniature, in [0, 1]; `null` for a week still ahead.
   */
  readonly spark: readonly (number | null)[];
}

/**
 * One of the ten parts of the rocket.
 */
export interface RocketPart {
  /**
   * Part index, one-based.
   */
  readonly index: number;

  /**
   * Two-digit part index.
   */
  readonly label: string;

  /**
   * Translated name of the part.
   */
  readonly name: string;

  /**
   * Built, next to build, or locked.
   */
  readonly state: 'built' | 'next' | 'locked';

  /**
   * Week the part was fitted in, `null` until it is.
   */
  readonly week: number | null;
}

/**
 * One row of the campaigns' ranking.
 */
export interface HistoryRow {
  /**
   * Rank by population, first being 1.
   */
  readonly rank: number;

  /**
   * Ordinal of the campaign.
   */
  readonly number: number;

  /**
   * Season or progress line under the number.
   */
  readonly subtitle: string;

  /**
   * Tier the squad was measured at.
   */
  readonly difficulty: CampaignDifficulty;

  /**
   * Inhabitants of the base.
   */
  readonly population: number;

  /**
   * Guardians defeated so far.
   */
  readonly guardiansDefeated: number;

  /**
   * Weeks played.
   */
  readonly weeksPlayed: number;

  /**
   * Wounded brought home.
   */
  readonly rescued: number;

  /**
   * Whether this is the campaign in progress.
   */
  readonly current: boolean;
}

/**
 * One campaign's curve, and the figure its legend shows.
 */
export interface HistoryCurve {
  /**
   * Chart series of the campaign.
   */
  readonly series: ChartSeries;

  /**
   * Figure shown in the legend.
   */
  readonly figure: number;

  /**
   * Whether this is the campaign in progress.
   */
  readonly current: boolean;
}

/**
 * Radius and colour of a planet, drawn from its category and its rank on the road.
 */
export function planetLook(week: CampaignWeek, final: boolean): { radius: number; hue: string } {
  const hues = [
    '#4a5b58',
    '#5a4c44',
    '#3f5566',
    '#56594a',
    '#6b5a3c',
    '#3a4f5a',
    '#4d5a4a',
    '#5c4a5a',
    '#3f5a5e',
    '#4a3d3a',
  ];
  const radius = final ? 44 : week.category === 'ELITE' ? 38 : week.category === 'MINOR' ? 30 : 33;
  return { radius, hue: hues[(week.weekIndex - 1) % hues.length] };
}

import {
  CampaignTier,
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
  readonly kind: 'now';
  readonly hitPointsLeft: number;
  readonly hitPoints: number;
  readonly breachPercent: number;

  /**
   * Whole days left before Sunday's extraction, zero on Sunday itself.
   */
  readonly daysLeft: number;
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
  readonly kind: 'settled';
  readonly defeated: boolean;

  /**
   * Weekday the guardian fell on, empty when it held.
   */
  readonly defeatedWeekday: string;
  readonly hitPoints: number;
  readonly hitPointsLeft: number;
  readonly breachPercent: number;
  readonly rescued: number;
  readonly spotted: number;
  readonly byChallenges: number;
  readonly limiter: ExtractionLimiter;

  /**
   * Inhabitants at the week's close, `null` when no day of it was replayed.
   */
  readonly population: number | null;
  readonly populationChange: number;
  readonly baseLoss: number;
}

/**
 * What can be said of a week still ahead.
 */
export interface AheadReport {
  readonly kind: 'ahead';
  readonly final: boolean;
  /** True on a closed campaign: the planet was never reached. */
  readonly unplayed: boolean;
}

export type PlanetReport = NowReport | SettledReport | AheadReport;

/**
 * One of the ten planets on the strip, and its report in the drawer.
 */
export interface Planet {
  readonly index: number;
  readonly label: string;
  readonly name: string;
  readonly category: GuardianCategory;
  readonly state: PlanetState;
  readonly final: boolean;

  /**
   * Share of the guardian's hit points taken, in [0, 1].
   */
  readonly advance: number;
  readonly radius: number;
  readonly hue: string;
  readonly stateLabel: string;
  readonly stateIcon: PlanetStateIcon;
  readonly report: PlanetReport;
}

/**
 * The rule of Sunday's settlement, with the figures of the week in progress in every term.
 */
export interface RescueLaw {
  readonly carry: number;
  readonly shelter: number;
  readonly wounded: number;
  readonly planetName: string;
  readonly breachPercent: number;
  readonly extracted: number;
  readonly byChallenges: number;
  readonly componentsPerRescue: number;
  readonly foodPerRescue: number;
  readonly hitPointsPerPercent: number;
}

/**
 * What a guardian left standing would cost the base as it stands, at three breakthroughs.
 */
export interface LossExample {
  readonly breachPercent: number;
  readonly lost: number;
}

/**
 * The three notes under the formula.
 */
export interface LawNotes {
  readonly tier: CampaignTier;
  readonly reference: number;
  readonly population: number;
  /** True when `population` is a sample figure, the base being empty. */
  readonly sample: boolean;
  readonly losses: readonly LossExample[];
}

/**
 * One stock of the base and the wounded it can pay for.
 */
export interface Tank {
  readonly stock: number;
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
  readonly food: Tank;
  readonly components: Tank;
  readonly dailyUpkeep: number;
  readonly wounded: number;
  readonly planetName: string;
  readonly rescued: number;
  readonly spotted: number;
  readonly byExtraction: number;
  readonly byChallenges: number;
  readonly byChallengesPercent: number;
  readonly leftBehind: number;

  /**
   * The three shares of the stacked bar, in percent of the spotted.
   */
  readonly shares: readonly [number, number, number];
  readonly guardiansDefeated: number;
  readonly weeksSettled: number;
  readonly limitedByFood: number;
  readonly limitedByComponents: number;
  readonly wholeGroup: number;
}

/**
 * One column head of the ledger: a planet.
 */
export interface LedgerColumn {
  readonly index: number;
  readonly name: string;
  readonly state: PlanetState;
}

/**
 * One week of one resource in the ledger.
 */
export interface LedgerCell {
  readonly index: number;
  readonly planetName: string;
  readonly kind: 'settled' | 'now' | 'ahead';
  readonly got: number;
  readonly spent: number;
  readonly carry: number;
  readonly carriedIn: number;

  /**
   * Stock at the end of the week's last replayed day.
   */
  readonly stock: number;
  readonly rescues: number;
  readonly stockRescues: number;

  /**
   * The three bars, as shares of the ledger's scale in [0, 1].
   */
  readonly gotShare: number;
  readonly spentShare: number;
  readonly carryShare: number;
}

/**
 * One row of the ledger: a resource across the ten weeks.
 */
export interface LedgerRow {
  readonly key: 'food' | 'components';
  readonly gained: number;
  readonly spent: number;
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
  readonly index: number;
  readonly label: string;
  readonly name: string;
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
  readonly rank: number;
  readonly number: number;
  readonly subtitle: string;
  readonly tier: CampaignTier;
  readonly population: number;
  readonly guardiansDefeated: number;
  readonly weeksPlayed: number;
  readonly rescued: number;
  readonly current: boolean;
}

/**
 * One campaign's curve, and the figure its legend shows.
 */
export interface HistoryCurve {
  readonly series: ChartSeries;
  readonly figure: number;
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

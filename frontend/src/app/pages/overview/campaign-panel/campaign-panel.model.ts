import { CampaignDifficulty } from '@core/campaign/campaign.model';
import { ChartSeries } from '@shared/chart/chart.model';

/**
 * Where one of the ten weeks stands: won, lost, being played, or still ahead.
 */
export type PlanetState = 'won' | 'lost' | 'now' | 'ahead';

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
   * Weeks played, the one in progress included.
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

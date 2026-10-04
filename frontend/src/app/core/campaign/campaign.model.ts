import { CampaignWeek, ExtractionLimiter } from './campaign-week.model';

/**
 * Mirrors the backend `CampaignStatus`; `OPENED` awaits its first Monday, `CLOSED` is frozen.
 */
export type CampaignStatus = 'OPENED' | 'RUNNING' | 'CLOSED';

/**
 * Difficulty chosen at opening, setting the reference and challenge grid; mirrors the backend.
 */
export type CampaignDifficulty = 'AMATEUR' | 'PRO';

/**
 * Start Monday; mirrors the backend. `CURRENT_WEEK` is retroactive, past days count.
 */
export type CampaignStartWeek = 'CURRENT_WEEK' | 'NEXT_WEEK';

/**
 * Base on the last replayed day; mirrors the backend `CampaignBaseResponse`.
 */
export interface CampaignBase {
  /**
   * Inhabitants of the base.
   */
  readonly population: number;

  /**
   * Food in stock.
   */
  readonly foodStock: number;

  /**
   * Components in stock.
   */
  readonly componentsStock: number;

  /**
   * Food the inhabitants eat every day.
   */
  readonly dailyUpkeep: number;

  /**
   * Food kept from Sunday's extraction: a week of upkeep.
   */
  readonly protectedFood: number;

  /**
   * Survivors the components in stock alone could bring home.
   */
  readonly rescuesByComponents: number;

  /**
   * Survivors the spendable food alone could bring home.
   */
  readonly rescuesByFood: number;

  /**
   * Inhabitants gained or lost over the last replayed day.
   */
  readonly populationChange: number;

  /**
   * Components one rescue costs the ship.
   */
  readonly componentsPerRescue: number;

  /**
   * Food one rescue costs the base.
   */
  readonly foodPerRescue: number;

  /**
   * Percent of the base killed by a guardian left at zero breakthrough.
   */
  readonly guardianLossPercent: number;
}

/**
 * Sunday outcome if the week ended now; mirrors the backend `CampaignForecastResponse`.
 */
export interface CampaignForecast {
  /**
   * One-based index of the week in the campaign.
   */
  readonly weekIndex: number;

  /**
   * Wounded waiting on the planet.
   */
  readonly woundedCount: number;

  /**
   * Wounded already rescued by challenges, kept whatever the guardian does.
   */
  readonly challengeRescued: number;

  /**
   * Wounded the ship would bring home at the current breakthrough.
   */
  readonly extractionRescued: number;

  /**
   * Wounded expected home tonight.
   */
  readonly rescued: number;

  /**
   * Wounded left on the planet.
   */
  readonly leftBehind: number;

  /**
   * What capped the extraction: a stock, the group, or nothing.
   */
  readonly limiter: ExtractionLimiter;
}

/**
 * Totals over the settled weeks; mirrors the backend `CampaignTotalsResponse`.
 */
export interface CampaignTotals {
  /**
   * Guardians defeated so far.
   */
  readonly guardiansDefeated: number;

  /**
   * Weeks already settled.
   */
  readonly weeksSettled: number;

  /**
   * Wounded brought home over the campaign.
   */
  readonly rescued: number;

  /**
   * Wounded rescued through validated challenges.
   */
  readonly challengeRescued: number;

  /**
   * Guardian damage dealt over the campaign.
   */
  readonly damage: number;

  /**
   * Food gained.
   */
  readonly foodGained: number;

  /**
   * Components gained.
   */
  readonly componentsGained: number;

  /**
   * Inhabitants lost to famine and guardians.
   */
  readonly inhabitantsLost: number;
}

/**
 * Campaign of `GET /api/campaign`; without any campaign, only `today` and `weeks` are set.
 */
export interface Campaign {
  /**
   * Internal identifier, `null` when no campaign exists.
   */
  readonly id: number | null;

  /**
   * Status of the campaign shown, or `null` when none exists.
   */
  readonly status: CampaignStatus | null;

  /**
   * Ordinal of the campaign, or `null` when none exists.
   */
  readonly number: number | null;

  /**
   * Difficulty of the campaign, or `null` when none exists.
   */
  readonly difficulty: CampaignDifficulty | null;

  /**
   * Reference figure, or `null` when none exists.
   */
  readonly reference: number | null;

  /**
   * Players on the roster, or `null` when none exists.
   */
  readonly rosterSize: number | null;

  /**
   * Monday of the first week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly firstWeekStart: string | null;

  /**
   * Monday of the tenth week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly lastWeekStart: string | null;

  /**
   * The day in progress, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly today: string;

  /**
   * One-based week in progress, or `null` before the campaign starts.
   */
  readonly currentWeekIndex: number | null;

  /**
   * The base as it stands, or `null` before the first replayed day.
   */
  readonly base: CampaignBase | null;

  /**
   * Forecast of the week in progress, `null` outside one.
   */
  readonly forecast: CampaignForecast | null;

  /**
   * The ten weeks, in order.
   */
  readonly weeks: readonly CampaignWeek[];

  /**
   * Campaign totals, or `null` before the first replayed day.
   */
  readonly totals: CampaignTotals | null;
}

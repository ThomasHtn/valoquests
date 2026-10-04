import { CampaignDifficulty } from './campaign.model';

/**
 * Closed campaign of `GET /api/campaign/history`.
 */
export interface CampaignHistory {
  /**
   * Internal identifier.
   */
  readonly id: number;

  /**
   * Ordinal of the campaign, first one being 1.
   */
  readonly number: number;

  /**
   * Difficulty the campaign was played at.
   */
  readonly difficulty: CampaignDifficulty;

  /**
   * Reference figure the squad was calibrated on.
   */
  readonly reference: number;

  /**
   * Players on the roster.
   */
  readonly rosterSize: number;

  /**
   * Monday of the first week, ISO date.
   */
  readonly firstWeekStart: string;

  /**
   * Monday of the last week, ISO date.
   */
  readonly lastWeekStart: string;

  /**
   * Day frozen on when stopped early, `null` if it ran to the end.
   */
  readonly stoppedOn: string | null;

  /**
   * Guardians defeated so far.
   */
  readonly guardiansDefeated: number;

  /**
   * Final population, the campaign's score.
   */
  readonly population: number;

  /**
   * Wounded brought home over the campaign.
   */
  readonly rescued: number;

  /**
   * Population at the end of each settled week, in week order.
   */
  readonly weeklyPopulation: readonly number[];
}

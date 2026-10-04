import { CampaignDifficulty, CampaignStatus } from '@core/campaign/campaign.model';

/**
 * Campaign returned by the open and stop commands; mirrors `CampaignAdminResponse`.
 */
export interface CampaignAdmin {
  /**
   * Internal identifier.
   */
  readonly id: number;

  /**
   * Ordinal of the campaign, first one being 1.
   */
  readonly number: number;

  /**
   * Lifecycle status of the campaign.
   */
  readonly status: CampaignStatus;

  /**
   * Monday of the first week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly firstWeekStart: string;

  /**
   * Monday of the tenth week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly lastWeekStart: string;

  /**
   * Day the campaign was frozen on when stopped early, or `null`.
   */
  readonly stoppedOn: string | null;

  /**
   * Reference figure the squad was calibrated on.
   */
  readonly reference: number;

  /**
   * Difficulty the campaign is played at.
   */
  readonly difficulty: CampaignDifficulty;

  /**
   * Players on the roster.
   */
  readonly rosterSize: number;
}

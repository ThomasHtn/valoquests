import { CampaignDifficulty, CampaignStatus } from '@core/campaign/campaign.model';

/**
 * A campaign the operator can act on, with the figures the decision is made against.
 */
export interface LiveCampaign {
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
   * Difficulty the campaign is played at.
   */
  readonly difficulty: CampaignDifficulty;

  /**
   * Reference the difficulty carries, every figure of the campaign being a multiple of it.
   */
  readonly reference: number;

  /**
   * Players on the roster.
   */
  readonly rosterSize: number;

  /**
   * First and last Monday, formatted.
   */
  readonly range: string;

  /**
   * Date the campaign starts, formatted.
   */
  readonly startsOn: string;

  /**
   * Week in progress, one-based.
   */
  readonly weekIndex: number;

  /**
   * One-based index of the day in the week.
   */
  readonly dayIndex: number;

  /**
   * Days until the campaign ends.
   */
  readonly daysLeft: number;
}

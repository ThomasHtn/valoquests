import { CampaignDifficulty, CampaignStatus } from '@core/campaign/campaign.model';

/**
 * Live campaign the operator can act on, with its key figures.
 */
export interface LiveCampaign {
  /**
   * Internal identifier.
   */
  readonly id: number;

  /**
   * One-based campaign ordinal.
   */
  readonly number: number;

  /**
   * Lifecycle status.
   */
  readonly status: CampaignStatus;

  /**
   * Difficulty.
   */
  readonly difficulty: CampaignDifficulty;

  /**
   * Difficulty reference, every campaign figure being a multiple of it.
   */
  readonly reference: number;

  /**
   * Players on the roster.
   */
  readonly rosterSize: number;

  /**
   * Formatted span, last Sunday included.
   */
  readonly range: string;

  /**
   * Formatted start date.
   */
  readonly startsOn: string;

  /**
   * One-based week in progress.
   */
  readonly weekIndex: number;

  /**
   * One-based day of the campaign.
   */
  readonly dayIndex: number;

  /**
   * Days until the campaign ends.
   */
  readonly daysLeft: number;
}

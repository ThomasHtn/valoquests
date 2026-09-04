import { CampaignStatus, WeeklyTitle } from '@core/campaign/campaign.model';

/**
 * What one operator brought to the week in progress.
 *
 * Mirrors `PlayerContributionResponse.WeekContributionResponse` from the backend.
 */
export interface WeekContribution {
  /**
   * Monday identifying the week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly weekStart: string;

  /**
   * 1-based ranking position, or `null` for an inactive operator.
   */
  readonly position: number | null;
  readonly guardianDamage: number;
  readonly food: number;
  readonly components: number;
  readonly matchCount: number;
  readonly activeDays: number;
  readonly streakDays: number;
  readonly challengePoints: number;
  readonly completedChallenges: number;
  readonly completedDailyChallenges: number;
  readonly totalPoints: number;
  readonly titles: readonly WeeklyTitle[];
}

/**
 * What one operator brought to the live campaign, over every day replayed so far.
 *
 * Mirrors `PlayerContributionResponse.CampaignContributionResponse` from the backend.
 */
export interface CampaignContribution {
  readonly campaignId: number;
  readonly campaignNumber: number;
  readonly status: CampaignStatus;
  readonly damage: number;
  readonly food: number;
  readonly components: number;
  readonly matchCount: number;
  readonly activeDays: number;
  readonly longestStreak: number;

  /**
   * Challenges validated over the campaign, weekly and daily alike.
   */
  readonly completedChallenges: number;

  /**
   * Survivors this operator's own challenges brought home.
   */
  readonly survivorsRescued: number;

  /**
   * Guardians this operator dealt the finishing blow to.
   */
  readonly finishingBlows: number;

  /**
   * How many times each title was earned over the campaign's weeks. A title never earned is
   * absent.
   */
  readonly titles: Partial<Record<WeeklyTitle, number>>;
}

/**
 * One operator's contribution at both scales, as returned by
 * `GET /api/players/{id}/contribution`.
 *
 * `week` is `null` before the week's first calculation; `campaign` is `null` between two
 * campaigns and for an operator the live one did not freeze on its roster.
 */
export interface PlayerContribution {
  readonly playerId: number;
  readonly week: WeekContribution | null;
  readonly campaign: CampaignContribution | null;
}

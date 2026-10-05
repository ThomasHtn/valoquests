import { WeeklyTitle } from './titles/campaign-title.model';

/**
 * One operator's day; mirrors the backend `CampaignPlayerDayResponse`.
 */
interface CampaignPlayerDay {
  /**
   * Internal identifier of the player.
   */
  readonly playerId: number;

  /**
   * Riot ID game name, before the `#`.
   */
  readonly gameName: string;

  /**
   * Riot ID tag line, after the `#`.
   */
  readonly tagLine: string;

  /**
   * Guardian damage dealt.
   */
  readonly damage: number;

  /**
   * Food.
   */
  readonly food: number;

  /**
   * Components.
   */
  readonly components: number;

  /**
   * Matches played.
   */
  readonly matchCount: number;

  /**
   * Matches reduced by the day's diminishing returns.
   */
  readonly reducedMatchCount: number;

  /**
   * Days of the week played up to this day.
   */
  readonly streakDays: number;

  /**
   * Bonus the streak grants, in percent.
   */
  readonly streakBonusPercent: number;
}

/**
 * Day of `GET /api/campaign/today`, empty outside a running campaign.
 */
export interface CampaignToday {
  /**
   * The day, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly day: string;

  /**
   * Guardian damage dealt.
   */
  readonly damage: number;

  /**
   * Food.
   */
  readonly food: number;

  /**
   * Components.
   */
  readonly components: number;

  /**
   * Operators who played today.
   */
  readonly presenceCount: number;

  /**
   * Players on the roster.
   */
  readonly rosterSize: number;

  /**
   * Food the base eats per day.
   */
  readonly dailyUpkeep: number;

  /**
   * Wounded today's components add to what the ship can carry.
   */
  readonly carryGained: number;

  /**
   * Wounded today's food adds to what the base can settle.
   */
  readonly shelterGained: number;

  /**
   * One line per player, best day first.
   */
  readonly players: readonly CampaignPlayerDay[];

  /**
   * Holder id of each week title so far; unheld titles are absent.
   */
  readonly titles: Partial<Record<WeeklyTitle, number>>;
}

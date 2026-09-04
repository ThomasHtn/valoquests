/**
 * Lifecycle state of a rescue campaign. Mirrors the backend `CampaignStatus` enum.
 *
 * `OPENED` is a campaign waiting for its first Monday; `RUNNING` is the ten weeks being played;
 * `CLOSED` is frozen, whether it went the distance or was stopped early.
 */
export type CampaignStatus = 'OPENED' | 'RUNNING' | 'CLOSED';

/**
 * Tier the squad was measured at when the campaign opened. Mirrors the backend `CampaignTier`.
 */
export type CampaignTier = 'AMATEUR' | 'NORMAL' | 'CONFIRMED' | 'ELITE';

/**
 * The four tiers from the lowest reference to the highest, in ladder order.
 */
export const CAMPAIGN_TIERS: readonly CampaignTier[] = ['AMATEUR', 'NORMAL', 'CONFIRMED', 'ELITE'];

/**
 * Weight class of a week's guardian. Mirrors the backend `GuardianCategory`.
 */
export type GuardianCategory = 'MINOR' | 'STANDARD' | 'ELITE';

/**
 * What capped Sunday's extraction. Mirrors the backend `ExtractionLimiter`.
 *
 * `NONE` means the whole group came home; `GROUP` means there was nobody left to rescue; the
 * two resources name the stock that ran out first.
 */
export type ExtractionLimiter = 'NONE' | 'GROUP' | 'FOOD' | 'COMPONENTS';

/**
 * Honorary title handed out on the week's ranking. Mirrors the backend `WeeklyTitle`.
 *
 * Mechanic for the most components, Quartermaster for the most food, Regular for the longest
 * streak, Scout for the most validated challenges. Ties award nothing.
 */
export type WeeklyTitle = 'MECHANIC' | 'QUARTERMASTER' | 'REGULAR' | 'SCOUT';

/**
 * The four titles, in the order the interface lists them.
 */
export const WEEKLY_TITLES: readonly WeeklyTitle[] = [
  'MECHANIC',
  'QUARTERMASTER',
  'REGULAR',
  'SCOUT',
];

/**
 * Number of weeks a campaign lasts. Mirrors the backend `CampaignSchedule.WEEK_COUNT`.
 */
export const CAMPAIGN_WEEK_COUNT = 10;

/**
 * The base as it stands on the last replayed day.
 *
 * Mirrors the backend `CampaignBaseResponse`.
 */
export interface CampaignBase {
  readonly population: number;
  readonly foodStock: number;
  readonly componentsStock: number;

  /**
   * Food the inhabitants eat every day.
   */
  readonly dailyUpkeep: number;

  /**
   * Food kept back from Sunday's extraction: a week of upkeep, never spent on a rescue.
   */
  readonly protectedFood: number;

  /**
   * How many survivors the components in stock could bring home on their own.
   */
  readonly rescuesByComponents: number;

  /**
   * How many survivors the spendable food could bring home on its own.
   */
  readonly rescuesByFood: number;
}

/**
 * One of the campaign's ten weeks: its planet, its guardian, and how Sunday settled it.
 *
 * Mirrors the backend `CampaignWeekResponse`. The guardian's name and description are only
 * revealed up to the current week; later ones are `null`.
 */
export interface CampaignWeek {
  readonly weekIndex: number;

  /**
   * Monday identifying the week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly weekStart: string;
  readonly planetName: string;
  readonly category: GuardianCategory;
  readonly guardianName: string | null;
  readonly guardianDescription: string | null;
  readonly guardianHitPoints: number;
  readonly damageDealt: number;

  /**
   * Damage dealt over hit points, capped at 100.
   */
  readonly progressPercent: number;
  readonly defeated: boolean;

  /**
   * Instant of the match that dealt the finishing blow, or `null` while the guardian stands.
   */
  readonly defeatedAt: string | null;
  readonly defeatedByPlayerId: number | null;

  /**
   * Survivors stranded on the planet, the most the week can bring home.
   */
  readonly woundedCount: number;
  readonly challengeRescued: number;
  readonly extractionRescued: number;
  readonly foodSpent: number;
  readonly componentsSpent: number;
  readonly limiter: ExtractionLimiter;

  /**
   * Inhabitants lost to a guardian left standing.
   */
  readonly baseLoss: number;
  readonly settled: boolean;
}

/**
 * Running totals over the settled weeks.
 *
 * Mirrors the backend `CampaignTotalsResponse`.
 */
export interface CampaignTotals {
  readonly guardiansDefeated: number;
  readonly weeksSettled: number;
  readonly rescued: number;
  readonly challengeRescued: number;
  readonly damage: number;
  readonly foodGained: number;
  readonly componentsGained: number;
  readonly inhabitantsLost: number;
}

/**
 * The campaign the site shows, as returned by `GET /api/campaign`.
 *
 * Every campaign-scoped field is `null` on a database that never had one; `today` and `weeks`
 * (then empty) are the only fields always set. The status tells which case the page is in.
 */
export interface Campaign {
  readonly status: CampaignStatus | null;
  readonly number: number | null;
  readonly tier: CampaignTier | null;
  readonly reference: number | null;
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
  readonly base: CampaignBase | null;
  readonly weeks: readonly CampaignWeek[];
  readonly totals: CampaignTotals | null;
}

/**
 * What one operator brought in on the day.
 *
 * Mirrors the backend `CampaignPlayerDayResponse`.
 */
export interface CampaignPlayerDay {
  readonly playerId: number;
  readonly gameName: string;
  readonly tagLine: string;
  readonly damage: number;
  readonly food: number;
  readonly components: number;
  readonly matchCount: number;

  /**
   * Matches priced below their full value by the day's diminishing returns.
   */
  readonly reducedMatchCount: number;
  readonly streakDays: number;
  readonly streakBonusPercent: number;
}

/**
 * The day in progress, as returned by `GET /api/campaign/today`.
 *
 * Empty (zeros, no operator) between campaigns and before the running one starts.
 */
export interface CampaignToday {
  /**
   * The day, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly day: string;
  readonly damage: number;
  readonly food: number;
  readonly components: number;

  /**
   * Operators who played today, over {@link rosterSize}.
   */
  readonly presenceCount: number;
  readonly rosterSize: number;
  readonly dailyUpkeep: number;
  readonly players: readonly CampaignPlayerDay[];

  /**
   * Who holds each of the week's titles so far, by player id. A title nobody holds is absent.
   */
  readonly titles: Partial<Record<WeeklyTitle, number>>;
}

/**
 * One closed campaign and how it ended, as returned by `GET /api/campaign/history`.
 */
export interface CampaignHistory {
  readonly number: number;
  readonly tier: CampaignTier;
  readonly reference: number;
  readonly rosterSize: number;
  readonly firstWeekStart: string;
  readonly lastWeekStart: string;

  /**
   * Day the campaign was frozen on when stopped early, or `null` when it went the distance.
   */
  readonly stoppedOn: string | null;
  readonly guardiansDefeated: number;

  /**
   * Final population of the base: the campaign's score.
   */
  readonly population: number;
  readonly rescued: number;

  /**
   * Population at the end of each settled week, in week order.
   */
  readonly weeklyPopulation: readonly number[];
}

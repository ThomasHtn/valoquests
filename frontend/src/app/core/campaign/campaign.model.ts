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
   * Share of the base a guardian left standing at zero breakthrough would kill, in percent.
   */
  readonly guardianLossPercent: number;
}

/**
 * The base at the close of one week, and what the week added to its stocks.
 *
 * Mirrors the backend `CampaignWeekBaseResponse`. For the week in progress the figures stop at
 * the last replayed day.
 */
export interface CampaignWeekBase {
  /**
   * Inhabitants of the base.
   */
  readonly population: number;

  /**
   * Inhabitants gained or lost since the previous week's close.
   */
  readonly populationChange: number;

  /**
   * Food in stock.
   */
  readonly foodStock: number;

  /**
   * Components in stock.
   */
  readonly componentsStock: number;

  /**
   * Food gained.
   */
  readonly foodGained: number;

  /**
   * Components gained.
   */
  readonly componentsGained: number;
}

/**
 * What Sunday would bring home if the week in progress ended on the base as it stands.
 *
 * Mirrors the backend `CampaignForecastResponse`.
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
   * Wounded the challenges have already brought home, acquired whatever the guardian does.
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
 * The match that brought a week's guardian down, as the mission report names it. Scores are
 * `null` for a mode that keeps none.
 */
export interface FatalBlow {
  /**
   * Name of the map.
   */
  readonly mapName: string | null;

  /**
   * Queue of the match, or `null`.
   */
  readonly gameMode: string | null;

  /**
   * Outcome of the match for the operator, or `null`.
   */
  readonly result: string | null;

  /**
   * Rounds won by the player’s team, or `null` for a mode without a round score.
   */
  readonly allyScore: number | null;

  /**
   * Rounds won by the opposing team, or `null` for a mode without a round score.
   */
  readonly enemyScore: number | null;

  /**
   * Agent played.
   */
  readonly agentName: string | null;
}

/**
 * One of the campaign's ten weeks: its planet, its guardian, and how Sunday settled it.
 *
 * Mirrors the backend `CampaignWeekResponse`. The guardian's name and description are only
 * revealed up to the current week; later ones are `null`.
 */
export interface CampaignWeek {
  /**
   * One-based index of the week in the campaign.
   */
  readonly weekIndex: number;

  /**
   * Monday identifying the week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly weekStart: string;

  /**
   * Name of the planet.
   */
  readonly planetName: string;

  /**
   * Weight class of the guardian.
   */
  readonly category: GuardianCategory;

  /**
   * Name of the guardian, or `null` while still hidden.
   */
  readonly guardianName: string | null;

  /**
   * Description of the guardian, or `null` while still hidden.
   */
  readonly guardianDescription: string | null;

  /**
   * Hit points the guardian started the week with.
   */
  readonly guardianHitPoints: number;

  /**
   * Raw in-game damage dealt.
   */
  readonly damageDealt: number;

  /**
   * Damage dealt over hit points, capped at 100.
   */
  readonly progressPercent: number;

  /**
   * Whether the guardian was defeated.
   */
  readonly defeated: boolean;

  /**
   * Instant of the match that dealt the finishing blow, or `null` while the guardian stands.
   */
  readonly defeatedAt: string | null;

  /**
   * Player who dealt the fatal blow, or `null` when the guardian stands.
   */
  readonly defeatedByPlayerId: number | null;

  /**
   * The match that dealt the finishing blow, or `null` while the guardian stands.
   */
  readonly fatalBlow: FatalBlow | null;

  /**
   * Survivors stranded on the planet, the most the week can bring home.
   */
  readonly woundedCount: number;

  /**
   * Wounded rescued through validated challenges.
   */
  readonly challengeRescued: number;

  /**
   * Wounded rescued by the Sunday extraction.
   */
  readonly extractionRescued: number;

  /**
   * Food the extraction consumed.
   */
  readonly foodSpent: number;

  /**
   * Components the extraction consumed.
   */
  readonly componentsSpent: number;

  /**
   * What capped the extraction, once settled.
   */
  readonly limiter: ExtractionLimiter;

  /**
   * Inhabitants lost to a guardian left standing.
   */
  readonly baseLoss: number;

  /**
   * Whether the week’s Sunday has been settled.
   */
  readonly settled: boolean;

  /**
   * The base at the week's close, `null` before its first replayed day.
   */
  readonly base: CampaignWeekBase | null;
}

/**
 * Running totals over the settled weeks.
 *
 * Mirrors the backend `CampaignTotalsResponse`.
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
 * The campaign the site shows, as returned by `GET /api/campaign`.
 *
 * Every campaign-scoped field is `null` on a database that never had one; `today` and `weeks`
 * (then empty) are the only fields always set. The status tells which case the page is in.
 */
export interface Campaign {
  /**
   * Internal identifier, `null` between two campaigns. What the backoffice deletes by.
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
   * Tier of the campaign, or `null` when none exists.
   */
  readonly tier: CampaignTier | null;

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

/**
 * What one operator brought in on the day.
 *
 * Mirrors the backend `CampaignPlayerDayResponse`.
 */
export interface CampaignPlayerDay {
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
   * Matches priced below their full value by the day's diminishing returns.
   */
  readonly reducedMatchCount: number;

  /**
   * Consecutive active days.
   */
  readonly streakDays: number;

  /**
   * Bonus the streak grants, in percent.
   */
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
   * Operators who played today, over {@link rosterSize}.
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
   * Who holds each of the week's titles so far, by player id. A title nobody holds is absent.
   */
  readonly titles: Partial<Record<WeeklyTitle, number>>;
}

/**
 * One closed campaign and how it ended, as returned by `GET /api/campaign/history`.
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
   * Tier the squad was measured at.
   */
  readonly tier: CampaignTier;

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
   * Day the campaign was frozen on when stopped early, or `null` when it went the distance.
   */
  readonly stoppedOn: string | null;

  /**
   * Guardians defeated so far.
   */
  readonly guardiansDefeated: number;

  /**
   * Final population of the base: the campaign's score.
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

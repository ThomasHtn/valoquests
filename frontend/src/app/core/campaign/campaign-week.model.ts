/**
 * Guardian weight class; mirrors the backend `GuardianCategory`.
 */
export type GuardianCategory = 'MINOR' | 'STANDARD' | 'ELITE';

/**
 * What capped Sunday's extraction; mirrors the backend `ExtractionLimiter`.
 */
export type ExtractionLimiter = 'NONE' | 'GROUP' | 'FOOD' | 'COMPONENTS';

/**
 * Base at a week's close (or last replayed day); mirrors `CampaignWeekBaseResponse`.
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
 * Match that brought a week's guardian down.
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
 * One campaign week; mirrors the backend `CampaignWeekResponse`.
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
   * Damage per replayed day, Monday first.
   */
  readonly dailyDamage: readonly number[];

  /**
   * Damage dealt over hit points, capped at 100.
   */
  readonly progressPercent: number;

  /**
   * Whether the guardian was defeated.
   */
  readonly defeated: boolean;

  /**
   * Instant of the finishing blow, `null` while the guardian stands.
   */
  readonly defeatedAt: string | null;

  /**
   * Player who dealt the fatal blow, or `null` when the guardian stands.
   */
  readonly defeatedByPlayerId: number | null;

  /**
   * Match of the finishing blow, `null` while the guardian stands.
   */
  readonly fatalBlow: FatalBlow | null;

  /**
   * Survivors stranded on the planet, the week's rescue cap.
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

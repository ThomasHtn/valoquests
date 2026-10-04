import { GuardianCategory } from '@core/campaign/campaign-week.model';

/**
 * One operator's segment of the duel track.
 */
export interface Strike {
  /**
   * Internal identifier of the player.
   */
  readonly playerId: number;

  /**
   * Display name of the operator.
   */
  readonly name: string;

  /**
   * Hit points dealt to the guardian, the segment's weight.
   */
  readonly damage: number;

  /**
   * Formatted damage for the bubble.
   */
  readonly damageLabel: string;

  /**
   * Share of the squad's damage, in whole percent.
   */
  readonly percent: number;

  /**
   * Challenge points earned this week; they never touch the guardian.
   */
  readonly challengePoints: number;

  /**
   * Bubble content as one sentence, for screen readers.
   */
  readonly summary: string;
}

/**
 * The week in progress.
 */
export interface Mission {
  /**
   * One-based index of the week in the campaign.
   */
  readonly weekIndex: number;

  /**
   * Name of the planet.
   */
  readonly planetName: string;

  /**
   * Weight class of the guardian.
   */
  readonly category: GuardianCategory;

  /**
   * Day of the week, Monday being 1.
   */
  readonly dayOfWeek: number;

  /**
   * Hit points the guardian still has.
   */
  readonly hitPointsLeft: number;

  /**
   * Hit points the guardian started with.
   */
  readonly hitPoints: number;

  /**
   * Share of the guardian's hit points taken, in percent.
   */
  readonly breachPercent: number;

  /**
   * Share of hit points left, in [0, 1].
   */
  readonly guardianLeft: number;

  /**
   * The fatal blow, `null` while the guardian stands.
   */
  readonly defeated: {
    /**
     * Formatted weekday of the fatal match.
     */
    readonly weekday: string;

    /**
     * Formatted time of the fatal match.
     */
    readonly time: string;

    /**
     * Operator who dealt the blow, `null` if unknown.
     */
    readonly by: string | null;
  } | null;

  /**
   * Wounded waiting on the planet.
   */
  readonly wounded: number;

  /**
   * Operators on the roster.
   */
  readonly crew: number;

  /**
   * Week end (Monday 00:00 Paris time), in epoch milliseconds.
   */
  readonly extractionDeadline: number;
}

/**
 * One operator's share of the squad's weekly contribution.
 */
export interface ContributionShare {
  /**
   * Internal identifier of the player.
   */
  readonly playerId: number;

  /**
   * Operator name.
   */
  readonly name: string;

  /**
   * Damage dealt to the week's guardian, streak bonus included.
   */
  readonly damage: number;

  /**
   * Ranking points banked by the challenges validated this week.
   */
  readonly challengePoints: number;

  /**
   * Damage plus challenge points, what the ranking orders on.
   */
  readonly total: number;

  /**
   * Share of the squad's contribution, in percent.
   */
  readonly sharePercent: number;
}

/**
 * What the squad has put into the week, on the guardian's hit point scale.
 */
export interface Contribution {
  /**
   * Damage dealt by the squad this week.
   */
  readonly total: number;

  /**
   * Hit points the guardian started with.
   */
  readonly hitPoints: number;

  /**
   * One segment per operator.
   */
  readonly shares: readonly ContributionShare[];
}

/**
 * What Sunday midnight can still change, measured against the current forecast.
 */
export interface SundayStakes {
  /**
   * Extra wounded rescued over the forecast if the guardian falls.
   */
  readonly gain: number;

  /**
   * Inhabitants killed if the guardian holds at the current breakthrough.
   */
  readonly loss: number;
}

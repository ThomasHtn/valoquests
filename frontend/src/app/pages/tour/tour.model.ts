import { ChallengeDifficulty } from '@core/challenges/challenge.model';
import { MatchResult } from '@core/matches/match-result.model';

/**
 * Identifier of a guided-tour step.
 *
 * Doubles as the step's translation namespace (`tour.steps.<id>.*`) and as the discriminant the
 * template switches on to render the matching illustration.
 */
export type TourStepId = 'intro' | 'base' | 'week' | 'resources' | 'challenges' | 'ranking';

/**
 * One match of the tracker excerpt the first step shows, in the profile's own row shape.
 */
export interface TourSampleMatch {
  /**
   * Map name, which also picks the thumbnail.
   */
  readonly mapName: string;

  /**
   * Agent played, which picks the portrait.
   */
  readonly agentName: string;

  /**
   * Game mode, as the `playerProfile.matches.gameMode` dictionary keys it.
   */
  readonly gameMode: string;

  /**
   * Local start time, already formatted.
   */
  readonly time: string;

  /**
   * Outcome, which colours the leading edge and the ally score.
   */
  readonly result: MatchResult;

  /**
   * Rounds won by the operator's team.
   */
  readonly allyScore: number;

  /**
   * Rounds won by the other team.
   */
  readonly enemyScore: number;

  /**
   * Kills.
   */
  readonly kills: number;

  /**
   * Deaths.
   */
  readonly deaths: number;

  /**
   * Assists.
   */
  readonly assists: number;

  /**
   * Damage the match dealt to the week's guardian.
   */
  readonly damage: number;

  /**
   * Share of its base value the day's ladder let the match keep, in percent.
   */
  readonly damagePercent: number;
}

/**
 * One weekly challenge of the fifth step, before translation.
 */
export interface TourSampleChallenge {
  /**
   * Translation key of the challenge's name and description (`tour.samples.challenges.<key>`).
   */
  readonly key: string;

  /**
   * Difficulty, which picks the tier numeral and the accent.
   */
  readonly difficulty: ChallengeDifficulty;

  /**
   * Wounded one operator brings back by validating it.
   */
  readonly survivors: number;

  /**
   * Whether each operator of the sample roster validated it, in roster order.
   */
  readonly done: readonly boolean[];
}

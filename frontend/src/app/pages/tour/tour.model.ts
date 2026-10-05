import { MatchResult } from '@core/matches/match-result.model';

/**
 * Tour step id, also its translation namespace (`tour.steps.<id>.*`).
 */
export type TourStepId = 'intro' | 'base' | 'week' | 'resources' | 'challenges' | 'ranking';

/**
 * A stretch of a step's claim, emphasized when it sat between `*` markers.
 */
export interface ClaimRun {
  /**
   * Text of the stretch, spaces kept.
   */
  readonly text: string;

  /**
   * Whether the stretch is emphasized.
   */
  readonly strong: boolean;
}

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
 * The day's challenge of the fifth step, before translation.
 */
export interface TourSampleDaily {
  /**
   * Translation key of the challenge's name and description (`tour.samples.challenges.<key>`).
   */
  readonly key: string;

  /**
   * Value to reach, in the challenge's own unit.
   */
  readonly target: number;

  /**
   * Wounded one operator brings back by validating it.
   */
  readonly survivors: number;

  /**
   * Each operator's progress, in roster order.
   */
  readonly progress: readonly number[];
}

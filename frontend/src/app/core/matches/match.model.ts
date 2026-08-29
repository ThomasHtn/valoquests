import { CompetitiveTier } from '@core/players/competitive-tier.model';
import { GameMode } from './game-mode.model';
import { MatchResult } from './match-result.model';

/**
 * One entry of a tracked player's paginated match history, as exposed by
 * `GET /api/players/{id}/matches`.
 *
 * Mirrors the backend `MatchResponse`.
 */
export interface Match {
  readonly id: number;
  readonly startedAt: string;
  readonly mapName: string;
  readonly gameMode: GameMode;
  readonly agentName: string;
  readonly result: MatchResult;

  /**
   * Rounds won by the player's team, or `null` when not reported for the mode.
   */
  readonly allyScore: number | null;

  /**
   * Rounds won by the opposing team, or `null` when not reported for the mode.
   */
  readonly enemyScore: number | null;
  readonly kills: number;
  readonly deaths: number;
  readonly assists: number;
  readonly kda: number;
  readonly acs: number;
  readonly adr: number;
  readonly headshotPercentage: number;
  readonly competitiveTier: CompetitiveTier;

  /**
   * Rank rating within the player's tier at the time of the match, or `null` when not available.
   */
  readonly rankRating: number | null;

  /**
   * Damage this match dealt to its week's boss, after the day's diminishing returns. `0` for a
   * match the ruleset does not value, such as a remake.
   *
   * The one figure tying a game to the ranking and the colony it fed; every other statistic here is
   * Valorant's own.
   */
  readonly valoquestsDamage: number;

  /**
   * Share of its base damage the match kept: `100` for a day's best games, less once the day's
   * ladder starts reducing them, and `0` for a match that never entered that ladder.
   */
  readonly damageCoefficientPercent: number;
}

import { CompetitiveTier } from '../players/competitive-tier.model';
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
}

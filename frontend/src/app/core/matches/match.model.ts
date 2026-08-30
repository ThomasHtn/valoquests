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

/**
 * Another tracked player's line in a match both of them played, as exposed within
 * `GET /api/players/{id}/matches/{matchId}`.
 *
 * The squad is small enough that two tracked players routinely land in the same lobby.
 *
 * Mirrors the backend `MatchTeammateResponse`.
 */
export interface MatchTeammate {
  readonly playerId: number;
  readonly displayName: string;

  /**
   * Agent name backing the other player's bundled avatar, or `null` when never synchronized.
   */
  readonly portrait: string | null;
  readonly agentName: string;

  /**
   * Whether the other player shared the requesting player's team.
   */
  readonly sameTeam: boolean;
  readonly result: MatchResult;
  readonly kills: number;
  readonly deaths: number;
  readonly assists: number;
  readonly acs: number;
}

/**
 * Full detail of one tracked player's match, as exposed by
 * `GET /api/players/{id}/matches/{matchId}`.
 *
 * A superset of {@link Match}: same identifier and figures the history list already shows, plus the
 * shot-type breakdown behind the headshot rate, the raw damage and round count, the match's
 * duration, and every other tracked player found in the same lobby.
 *
 * Mirrors the backend `MatchDetailResponse`.
 */
export interface MatchDetail {
  readonly id: number;
  readonly startedAt: string;

  /**
   * Match duration in seconds, or `null` when Henrik did not report it.
   */
  readonly durationSeconds: number | null;
  readonly mapName: string;
  readonly gameMode: GameMode;
  readonly agentName: string;
  readonly result: MatchResult;
  readonly allyScore: number | null;
  readonly enemyScore: number | null;
  readonly kills: number;
  readonly deaths: number;
  readonly assists: number;
  readonly kda: number;
  readonly acs: number;
  readonly adr: number;
  readonly headshots: number;
  readonly bodyshots: number;
  readonly legshots: number;
  readonly headshotPercentage: number;
  readonly damageDealt: number;
  readonly roundsPlayed: number;
  readonly mvp: boolean;
  readonly competitiveTier: CompetitiveTier;
  readonly valoquestsDamage: number;
  readonly damageCoefficientPercent: number;

  /**
   * Every other tracked player found in the same match, on either team.
   */
  readonly teammates: readonly MatchTeammate[];
}

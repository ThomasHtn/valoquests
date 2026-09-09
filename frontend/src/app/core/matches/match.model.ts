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
  /**
   * Internal identifier.
   */
  readonly id: number;

  /**
   * Start instant, as an ISO-8601 string.
   */
  readonly startedAt: string;

  /**
   * Name of the map.
   */
  readonly mapName: string;

  /**
   * Queue the match was played in.
   */
  readonly gameMode: GameMode;

  /**
   * Agent played.
   */
  readonly agentName: string;

  /**
   * Outcome for the player.
   */
  readonly result: MatchResult;

  /**
   * Rounds won by the player's team, or `null` when not reported for the mode.
   */
  readonly allyScore: number | null;

  /**
   * Rounds won by the opposing team, or `null` when not reported for the mode.
   */
  readonly enemyScore: number | null;

  /**
   * Kills scored.
   */
  readonly kills: number;

  /**
   * Deaths suffered.
   */
  readonly deaths: number;

  /**
   * Assists given.
   */
  readonly assists: number;

  /**
   * Ratio of kills and assists to deaths.
   */
  readonly kda: number;

  /**
   * Average combat score.
   */
  readonly acs: number;

  /**
   * Average damage per round.
   */
  readonly adr: number;

  /**
   * Share of hits that landed on the head, in percent.
   */
  readonly headshotPercentage: number;

  /**
   * Competitive rank held.
   */
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
  /**
   * Internal identifier of the player.
   */
  readonly playerId: number;

  /**
   * Name shown across the application.
   */
  readonly displayName: string;

  /**
   * Agent name backing the other player's bundled avatar, or `null` when never synchronized.
   */
  readonly portrait: string | null;

  /**
   * Agent played.
   */
  readonly agentName: string;

  /**
   * Whether the other player shared the requesting player's team.
   */
  readonly sameTeam: boolean;

  /**
   * Outcome for the teammate, always the same as the player’s.
   */
  readonly result: MatchResult;

  /**
   * Kills scored.
   */
  readonly kills: number;

  /**
   * Deaths suffered.
   */
  readonly deaths: number;

  /**
   * Assists given.
   */
  readonly assists: number;

  /**
   * Average combat score.
   */
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
  /**
   * Internal identifier.
   */
  readonly id: number;

  /**
   * Start instant, as an ISO-8601 string.
   */
  readonly startedAt: string;

  /**
   * Match duration in seconds, or `null` when Henrik did not report it.
   */
  readonly durationSeconds: number | null;

  /**
   * Name of the map.
   */
  readonly mapName: string;

  /**
   * Queue the match was played in.
   */
  readonly gameMode: GameMode;

  /**
   * Agent played.
   */
  readonly agentName: string;

  /**
   * Outcome of the match for the player.
   */
  readonly result: MatchResult;

  /**
   * Rounds won by the player’s team, or `null` for a mode without a round score.
   */
  readonly allyScore: number | null;

  /**
   * Rounds won by the opposing team, or `null` for a mode without a round score.
   */
  readonly enemyScore: number | null;

  /**
   * Kills scored.
   */
  readonly kills: number;

  /**
   * Deaths suffered.
   */
  readonly deaths: number;

  /**
   * Assists given.
   */
  readonly assists: number;

  /**
   * Ratio of kills and assists to deaths.
   */
  readonly kda: number;

  /**
   * Average combat score.
   */
  readonly acs: number;

  /**
   * Average damage per round.
   */
  readonly adr: number;

  /**
   * Hits on the head.
   */
  readonly headshots: number;

  /**
   * Hits on the body.
   */
  readonly bodyshots: number;

  /**
   * Hits on the legs.
   */
  readonly legshots: number;

  /**
   * Share of hits that landed on the head, in percent.
   */
  readonly headshotPercentage: number;

  /**
   * Raw in-game damage dealt.
   */
  readonly damageDealt: number;

  /**
   * Rounds the match lasted.
   */
  readonly roundsPlayed: number;

  /**
   * Whether the player was the match MVP.
   */
  readonly mvp: boolean;

  /**
   * Competitive rank held.
   */
  readonly competitiveTier: CompetitiveTier;

  /**
   * Guardian damage the match was worth once the day’s ladder applied.
   */
  readonly valoquestsDamage: number;

  /**
   * Share of the raw value the ladder let through, in percent.
   */
  readonly damageCoefficientPercent: number;

  /**
   * Every other tracked player found in the same match, on either team.
   */
  readonly teammates: readonly MatchTeammate[];
}

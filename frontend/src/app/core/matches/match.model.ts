import { CompetitiveTier } from '@core/players/competitive-tier/player-competitive-tier.model';
import { GameMode } from './game-mode/match-game-mode.model';
import { MatchResult } from './match-result.model';

/**
 * One entry of a player's match history. Mirrors the backend `MatchResponse`.
 */
export interface Match {
  /**
   * Internal identifier.
   */
  readonly id: number;

  /**
   * Start instant, ISO-8601.
   */
  readonly startedAt: string;

  /**
   * Map name.
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
   * Rounds won by the player's team, `null` when the mode reports none.
   */
  readonly allyScore: number | null;

  /**
   * Rounds won by the opposing team, `null` when the mode reports none.
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
   * Kills per death, the kill total when deathless (as K/D challenges read it).
   */
  readonly kd: number;

  /**
   * Average combat score, `null` when the mode reports none.
   */
  readonly acs: number | null;

  /**
   * Average damage per round, `null` when the mode reports none.
   */
  readonly adr: number | null;

  /**
   * Headshot share in percent, `null` when Henrik reported no shot data.
   */
  readonly headshotPercentage: number | null;

  /**
   * Competitive rank held.
   */
  readonly competitiveTier: CompetitiveTier;

  /**
   * Rank rating within the tier at match time, `null` when unavailable.
   */
  readonly rankRating: number | null;

  /**
   * Boss damage after the day's diminishing returns, `0` for an unvalued match (remake).
   */
  readonly valoquestsDamage: number;

  /**
   * Share of base damage kept, in percent: `0` for a match outside the day's ladder.
   */
  readonly damageCoefficientPercent: number;
}

/**
 * Another tracked player in the same match. Mirrors the backend `MatchTeammateResponse`.
 */
interface MatchTeammate {
  /**
   * Player identifier.
   */
  readonly playerId: number;

  /**
   * Display name.
   */
  readonly displayName: string;

  /**
   * Agent name backing the bundled avatar, `null` when never synchronized.
   */
  readonly portrait: string | null;

  /**
   * Agent played.
   */
  readonly agentName: string;

  /**
   * Whether they shared the requesting player's team.
   */
  readonly sameTeam: boolean;

  /**
   * Outcome for the teammate.
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
   * Average combat score, `null` when the mode reports none.
   */
  readonly acs: number | null;
}

/**
 * Full detail of one match, superset of `Match`. Mirrors the backend `MatchDetailResponse`.
 */
export interface MatchDetail {
  /**
   * Internal identifier.
   */
  readonly id: number;

  /**
   * Start instant, ISO-8601.
   */
  readonly startedAt: string;

  /**
   * Duration in seconds, `null` when Henrik did not report it.
   */
  readonly durationSeconds: number | null;

  /**
   * Map name.
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
   * Rounds won by the player's team, `null` when the mode has no round score.
   */
  readonly allyScore: number | null;

  /**
   * Rounds won by the opposing team, `null` when the mode has no round score.
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
   * Kills per death, the kill total when deathless (as K/D challenges read it).
   */
  readonly kd: number;

  /**
   * Average combat score, `null` when the mode reports none.
   */
  readonly acs: number | null;

  /**
   * Average damage per round, `null` when the mode reports none.
   */
  readonly adr: number | null;

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
   * Headshot share in percent, `null` when Henrik reported no shot data.
   */
  readonly headshotPercentage: number | null;

  /**
   * Raw in-game damage dealt.
   */
  readonly damageDealt: number;

  /**
   * Rounds played.
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
   * Guardian damage after the day's ladder.
   */
  readonly valoquestsDamage: number;

  /**
   * Share of the raw value the ladder let through, in percent.
   */
  readonly damageCoefficientPercent: number;

  /**
   * Other tracked players in the match, either team.
   */
  readonly teammates: readonly MatchTeammate[];
}

/**
 * A match's round score, split so each side gets its own styling.
 */
export interface MatchScore {
  /**
   * Rounds won by the player's team.
   */
  readonly ally: number;

  /**
   * Rounds won by the opposing team.
   */
  readonly enemy: number;
}

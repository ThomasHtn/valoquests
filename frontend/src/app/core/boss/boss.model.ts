/**
 * Weight class of a boss, governing its base hit points.
 *
 * Mirrors the backend `BossCategory` enum.
 */
export type BossCategory = 'MINOR' | 'STANDARD' | 'ELITE';

/**
 * Catalogue identity of a drawn boss.
 *
 * Mirrors `BossResponse` from the backend.
 */
export interface Boss {
  readonly code: string;
  readonly name: string;
  readonly description: string;

  /**
   * Visual asset reference, or `null` until real artwork replaces the provisional catalogue.
   */
  readonly imageUrl: string | null;
  readonly category: BossCategory;
}

/**
 * Active weekly boss confrontation.
 *
 * Mirrors the backend `CurrentBossResponse` returned by `GET /api/boss/current`.
 */
export interface CurrentBoss {
  /**
   * Monday identifying the active week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly weekStart: string;

  /**
   * Sunday identifying the active week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly weekEnd: string;
  readonly boss: Boss;
  readonly effectiveHp: number;
  readonly totalDamageDealt: number;

  /**
   * Sequential number of the ten-week run the campaign is in.
   *
   * The campaign is scoped to this run: `GET /api/boss/history` only returns the fights sharing it,
   * so a new run opens on an empty history and the map starts again from the week being fought.
   * Unlike the Valorant act it replaces, a run has a known length from the moment it opens, which
   * is what lets the map draw a fixed number of hexagons.
   */
  readonly runNumber: number;

  /**
   * Position of the active week inside that run, from one.
   */
  readonly runWeekIndex: number;

  /**
   * Number of weeks a run spans, and therefore the number of hexagons the map draws.
   */
  readonly runWeekCount: number;
}

/**
 * Finalized boss confrontation for one historical week.
 *
 * Mirrors the backend `BossHistoryWeekResponse` from `GET /api/boss/history`.
 */
export interface BossHistoryWeek {
  /**
   * Monday identifying the week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly weekStart: string;

  /**
   * Sunday identifying the week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly weekEnd: string;

  /**
   * Position of the week inside its run, from one — where the campaign map places it.
   *
   * Sent rather than inferred from the fight's position in the history: a week that closed without a
   * fight leaves a hole, and a map placing fights by list position would shift every reward after it
   * onto the wrong week.
   */
  readonly runWeekIndex: number;
  readonly boss: Boss;
  readonly effectiveHp: number;
  readonly totalDamageDealt: number;
  readonly defeated: boolean;
  readonly defeatedByPlayerId: number | null;
  readonly defeatedByPlayerDisplayName: string | null;
}

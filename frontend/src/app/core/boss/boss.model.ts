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
  readonly baseHp: number;
  readonly difficultyModifierPercent: number;
  readonly effectiveHp: number;
  readonly totalDamageDealt: number;
  readonly enteringWinStreak: number;
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
  readonly boss: Boss;
  readonly effectiveHp: number;
  readonly totalDamageDealt: number;
  readonly defeated: boolean;
  readonly defeatedByPlayerId: number | null;
  readonly defeatedByPlayerDisplayName: string | null;
  readonly winStreak: number;
}

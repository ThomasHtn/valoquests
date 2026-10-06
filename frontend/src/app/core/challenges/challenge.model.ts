/**
 * Weekly challenge tier, setting its weight; mirrors the backend `ChallengeTier`.
 */
export type ChallengeTier = 'EASY' | 'NORMAL' | 'MEDIUM' | 'HARD' | 'VERY_HARD';

/**
 * Draw cadence (five weekly on Monday, one daily each morning); mirrors `ChallengeCadence`.
 */
export type ChallengeCadence = 'WEEKLY' | 'DAILY';

/**
 * Fields shared by drawn and catalogue challenges; targets come scaled by the backend.
 */
interface ChallengeIdentity {
  /**
   * Internal identifier.
   */
  readonly id: number;

  /**
   * Stable catalogue code (`EASY_DM_HEADSHOTS`), the key to per-challenge visuals.
   */
  readonly code: string;

  /**
   * Translated name.
   */
  readonly name: string;

  /**
   * Translated description, in the catalogue's base wording.
   */
  readonly description: string;

  /**
   * Daily or weekly.
   */
  readonly cadence: ChallengeCadence;

  /**
   * Weekly tier, `null` for a daily.
   */
  readonly tier: ChallengeTier | null;

  /**
   * Metric(s), joined with `" + "` when composite (`"KILLS + MATCHES_PLAYED"`).
   */
  readonly metric: string;

  /**
   * Resolved target progress is measured against, `null` for a composite challenge.
   */
  readonly targetValue: number | null;

  /**
   * Survivors one operator rescues by validating it, at its draw week; also its weekly ranking points.
   */
  readonly survivors: number;
}

/**
 * Squad progress on a drawn challenge; mirrors `ChallengeProgressResponse`.
 */
export interface ChallengeProgress extends ChallengeIdentity {
  /**
   * Decision day of a daily (`YYYY-MM-DD`), `null` for a weekly.
   */
  readonly day: string | null;

  /**
   * Ids of the active operators who validated it, matched against the roster.
   */
  readonly completedPlayerIds: readonly number[];

  /**
   * Each active operator's progress, in roster order.
   */
  readonly players: readonly PlayerChallengeProgress[];
}

/**
 * One active operator on one challenge; mirrors `PlayerProgressResponse`.
 */
interface PlayerChallengeProgress {
  /**
   * Player id, one of the roster.
   */
  readonly playerId: number;

  /**
   * Progress, zero until evaluated.
   */
  readonly currentValue: number;

  /**
   * Whether the player validated it.
   */
  readonly completed: boolean;
}

/**
 * One active operator, the unit completions count; mirrors `RosterPlayerResponse`.
 */
export interface RosterPlayer {
  /**
   * Internal identifier.
   */
  readonly id: number;

  /**
   * Display name.
   */
  readonly displayName: string;

  /**
   * Chosen agent portrait, `null` when none.
   */
  readonly portrait: string | null;
}

/**
 * Active week's draws with progress; mirrors `CurrentChallengesResponse`.
 */
export interface CurrentChallenges {
  /**
   * Monday of the active week (`YYYY-MM-DD`).
   */
  readonly weekStart: string;

  /**
   * Sunday of the active week (`YYYY-MM-DD`).
   */
  readonly weekEnd: string;

  /**
   * Current day (`YYYY-MM-DD`).
   */
  readonly today: string;

  /**
   * Last successful player sync (ISO-8601 instant), `null` when none yet.
   */
  readonly lastSuccessfulSynchronizationAt: string | null;

  /**
   * Active operators every challenge applies to, in roster order.
   */
  readonly roster: readonly RosterPlayer[];

  /**
   * The week's five, one per difficulty, easiest first.
   */
  readonly challenges: readonly ChallengeProgress[];

  /**
   * The week's dailies so far, oldest first (today's last).
   */
  readonly dailies: readonly ChallengeProgress[];
}

/**
 * One catalogue challenge outside any draw; mirrors `ChallengeCatalogueEntry`.
 */
export interface ChallengeCatalogueEntry extends ChallengeIdentity {
  /**
   * Whether only competitive matches count.
   */
  readonly competitiveOnly: boolean;
}

/**
 * Full catalogue at the reference in force; mirrors `ChallengeCatalogueResponse`.
 */
export interface ChallengeCatalogue {
  /**
   * Weekly reference: the live campaign's, else the last closed one's, else the floor.
   */
  readonly reference: number;

  /**
   * Catalogue entries.
   */
  readonly challenges: readonly ChallengeCatalogueEntry[];
}

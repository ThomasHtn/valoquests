import { Match } from '@core/matches/match.model';

/**
 * Player a history row belongs to, shown only on the squad's shared history.
 */
export interface MatchOwner {
  /**
   * Player identifier, for the link to the match.
   */
  readonly id: number;

  /**
   * Display name.
   */
  readonly name: string;

  /**
   * Avatar URL, `null` for the placeholder icon.
   */
  readonly portrait: string | null;
}

/**
 * One history row, with its player on the squad's shared history.
 */
export interface HistoryMatch extends Match {
  /**
   * Who played the match, absent on a player's own history.
   */
  readonly player?: MatchOwner;
}

/**
 * One day of a match history with its own record, so an evening reads as one session.
 */
export interface MatchDay<T extends Match = Match> {
  /**
   * Day as `YYYY-MM-DD` in the reader's time zone, also the tracking key.
   */
  readonly dayKey: string;

  /**
   * Formatted day label, e.g. `"28/07/2026"`.
   */
  readonly dateLabel: string;

  /**
   * Matches won that day.
   */
  readonly wins: number;

  /**
   * Matches lost that day; draws, remakes and unknown results count as neither.
   */
  readonly losses: number;

  /**
   * The day's matches, in API order.
   */
  readonly matches: readonly T[];

  /**
   * Day kills over day deaths, so it matches the K/D/A beside it.
   */
  readonly avgKd: number;

  /**
   * Average headshot percentage of matches reporting one, `null` when none does.
   */
  readonly avgHeadshotPercentage: number | null;

  /**
   * Average damage per round of matches reporting one, `null` when none does.
   */
  readonly avgAdr: number | null;

  /**
   * Average combat score of matches reporting one, `null` when none does.
   */
  readonly avgAcs: number | null;

  /**
   * Kills summed over the day.
   */
  readonly totalKills: number;

  /**
   * Deaths summed over the day.
   */
  readonly totalDeaths: number;

  /**
   * Assists summed over the day.
   */
  readonly totalAssists: number;

  /**
   * ValoQuests damage summed over the day, the scale diminishing returns work on.
   */
  readonly totalValoquestsDamage: number;
}

/**
 * A {@link MatchDay} before `withDayAverages` derives its averages and totals.
 */
export type MatchDayGroup<T extends Match> = Omit<
  MatchDay<T>,
  | 'avgAcs'
  | 'avgAdr'
  | 'avgHeadshotPercentage'
  | 'avgKd'
  | 'totalAssists'
  | 'totalDeaths'
  | 'totalKills'
  | 'totalValoquestsDamage'
>;

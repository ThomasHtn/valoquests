import { Match } from '@core/matches/match.model';

/**
 * The player a history row belongs to, shown on the squad's shared history only: a player's own
 * history never names them on every row.
 */
export interface MatchOwner {
  /**
   * Internal identifier of the player, for the link out to the match.
   */
  readonly id: number;

  /**
   * Name shown across the application.
   */
  readonly name: string;

  /**
   * Resolved avatar URL, or `null` for the placeholder icon.
   */
  readonly portrait: string | null;
}

/**
 * One row of a match history: the match, named after its player on the squad's shared history.
 */
export interface HistoryMatch extends Match {
  /**
   * Who played the match, absent on a player's own history.
   */
  readonly player?: MatchOwner;
}

/**
 * One day of a match history, with the day's own record.
 *
 * The history is read as a series of sessions rather than as a flat list: grouping by day is what
 * lets a reader recognize an evening of play and relate it to the week of challenges in progress.
 */
export interface MatchDay<T extends Match = Match> {
  /**
   * Calendar day the matches were played on, as `YYYY-MM-DD` in the reader's timezone. Used as the
   * group's tracking key.
   */
  readonly dayKey: string;

  /**
   * Pre-formatted day label, e.g. `"28/07/2026"`.
   */
  readonly dateLabel: string;

  /**
   * Matches won that day.
   */
  readonly wins: number;

  /**
   * Matches lost that day. Draws, remakes and unknown results count as neither.
   */
  readonly losses: number;

  /**
   * The day's matches, in the order the API returned them.
   */
  readonly matches: readonly T[];

  /**
   * K/D ratio of the day: its kills over its deaths, so it matches the K/D/A beside it.
   */
  readonly avgKd: number;

  /**
   * Average headshot percentage across the day's matches that report one, or `null` when none does.
   */
  readonly avgHeadshotPercentage: number | null;

  /**
   * Average damage per round across the day's matches that report one, `null` when none does.
   */
  readonly avgAdr: number | null;

  /**
   * Average combat score across the day's matches that report one, `null` when none does.
   */
  readonly avgAcs: number | null;

  /**
   * Kills summed across the day's matches.
   */
  readonly totalKills: number;

  /**
   * Deaths summed across the day's matches.
   */
  readonly totalDeaths: number;

  /**
   * Assists summed across the day's matches.
   */
  readonly totalAssists: number;

  /**
   * ValoQuests damage summed across the day's matches.
   *
   * Totalled at the day rather than at the week because the day is the scale the ruleset's
   * diminishing returns work on: this is exactly what an evening of play was worth.
   */
  readonly totalValoquestsDamage: number;
}

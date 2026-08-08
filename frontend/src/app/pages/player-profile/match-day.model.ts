import { Match } from '@core/matches/match.model';

/**
 * One day of a player's match history, with the day's own record.
 *
 * The history is read as a series of sessions rather than as a flat list: grouping by day is what
 * lets a reader recognize an evening of play and relate it to the week of challenges in progress.
 */
export interface MatchDay {
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
  readonly matches: readonly Match[];

  /**
   * Average KDA ratio across the day's matches.
   */
  readonly avgKda: number;

  /**
   * Average headshot percentage across the day's matches.
   */
  readonly avgHeadshotPercentage: number;

  /**
   * Average damage per round across the day's matches.
   */
  readonly avgAdr: number;

  /**
   * Average combat score across the day's matches.
   */
  readonly avgAcs: number;

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
}

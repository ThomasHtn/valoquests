import { WeeklyTitle } from '@core/campaign/titles/campaign-title.model';
import { ResultTone } from '@core/matches/display/match-visual.model';

/**
 * One figure of the match's stat grid, ready to display.
 */
export interface MatchFigure {
  /**
   * Translation key of the caption.
   */
  readonly labelKey: string;

  /**
   * Translation key of the caption's tooltip, `null` for a self-explaining figure.
   */
  readonly hintKey: string | null;

  /**
   * Formatted figure.
   */
  readonly value: string;

  /**
   * CSS colour judging the figure, bound as `--tone`; `null` keeps the tile's own.
   */
  readonly tone: string | null;
}

/**
 * Hits of one body zone, behind the headshot rate.
 */
export interface MatchShotCount {
  /**
   * Translation key of the zone.
   */
  readonly labelKey: string;

  /**
   * Hits registered there.
   */
  readonly count: number;
}

/**
 * Another tracked player of the lobby, ready to display.
 */
export interface MatchTeammateRow {
  /**
   * Player id, the profile link's target.
   */
  readonly playerId: number;

  /**
   * Name shown across the application.
   */
  readonly displayName: string;

  /**
   * Bundled avatar URL, `null` for the fallback icon.
   */
  readonly avatarUrl: string | null;

  /**
   * Whether the player won the last finalized week.
   */
  readonly isChampion: boolean;

  /**
   * Weekly title held this week, `null` when none.
   */
  readonly title: WeeklyTitle | null;

  /**
   * Agent played in the match.
   */
  readonly agentName: string;

  /**
   * Translation key saying which side the player was on.
   */
  readonly sideKey: string;

  /**
   * Kills, deaths and assists as `12/8/4`.
   */
  readonly kda: string;

  /**
   * Tone of the player's result.
   */
  readonly resultTone: ResultTone;
}

import { WeeklyTitle } from '@core/campaign/campaign.model';
import { TitleVisual } from '@core/campaign/campaign-visual.utils';
import { ChallengeTier } from '@core/challenges/challenge-visual.model';
import { ChallengeRingCell } from '@shared/challenge-ring/challenge-ring';

/**
 * A title an operator holds on the board, with the icon and colour it is drawn in.
 */
export interface BoardTitle extends TitleVisual {
  readonly key: WeeklyTitle;

  /**
   * The figure the title was awarded on, worded ("2 996 composants"), or `null` when the week
   * kept no such figure.
   */
  readonly measure: string | null;
}

/**
 * One of the week's four titles, as the line under the podium states it: its holder, or why it
 * was not awarded.
 */
export interface WeekTitleLine extends TitleVisual {
  readonly key: WeeklyTitle;
  readonly holder: string | null;

  /**
   * The holder's figure, or the tied figure nobody won outright; empty when nobody scored.
   */
  readonly detail: string;
}

/**
 * One challenge column of a live board: the tier badge the header shows in place of the
 * challenge's name, which would not fit at this width. The name stays one hover away.
 */
export interface BoardColumn {
  readonly id: number;
  readonly mark: ChallengeTier;
  readonly barClass: string;
  readonly iconClass: string;
  readonly tip: string;
}

/**
 * One operator's progress toward one weekly challenge: a ring closing toward its target on the
 * wide board, a bar under the name on the narrow one.
 */
export interface BoardProgress extends ChallengeRingCell {
  readonly id: number;
  readonly mark: ChallengeTier;

  /**
   * The challenge's name alone, as the narrow board lists it.
   */
  readonly label: string;

  /**
   * The name, what had to be done and the metric, as the header's column names it.
   */
  readonly name: string;
  readonly barClass: string;

  /**
   * The whole sentence the cell abbreviates: the name, where the operator stands, the wounded.
   */
  readonly tip: string;
}

/**
 * One row of the board: where the operator stands and what got them there.
 */
export interface BoardRow {
  readonly playerId: number;
  readonly name: string;
  readonly portrait: string | null;

  /**
   * 1-based position, or `null` for an operator out of the campaign, who is tracked but never
   * takes a slot.
   */
  readonly position: number | null;

  /**
   * Places climbed since the last calculation, negative when lost. Zero on a closed week.
   */
  readonly variation: number;
  readonly isChampion: boolean;
  readonly total: number;
  readonly damage: number;
  readonly challengePoints: number;
  readonly titles: readonly BoardTitle[];

  /**
   * One cell per weekly challenge of the board, or `null` on a closed week, whose progress was
   * not kept.
   */
  readonly progress: readonly BoardProgress[] | null;
}

/**
 * One week the board can show: the live one, or a closed one browsed back to.
 */
export interface BoardWeek {
  /**
   * Monday identifying the week, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly weekStart: string;
  readonly live: boolean;

  /**
   * Position in the campaign, or `null` for a week outside one.
   */
  readonly weekIndex: number | null;

  /** The challenge columns, in the rows' order; empty on a closed week, which keeps no progress. */
  readonly columns: readonly BoardColumn[];
  readonly ranked: readonly BoardRow[];
  readonly unranked: readonly BoardRow[];
}

/**
 * The operator who finished a closed week first, as the week picker names it.
 */
export interface WeekWinner {
  readonly name: string;
  readonly portrait: string | null;
}

/**
 * One week the picker offers, newest first.
 */
export interface WeekOption {
  readonly weekStart: string;

  /**
   * Monday to Sunday, the month spelled once when both days share it.
   */
  readonly label: string;

  /**
   * Position in its campaign, or `null` for a week outside one.
   */
  readonly index: number | null;

  /**
   * What the option belongs to — a campaign's id, or `null` outside one — so the picker can rule
   * off one run of weeks from the next without naming them.
   */
  readonly group: number | null;
  readonly live: boolean;

  /**
   * Who won the week; `null` while it is still running or when nobody was ranked.
   */
  readonly winner: WeekWinner | null;
}

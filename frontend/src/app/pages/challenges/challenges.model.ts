import { ChallengeCatalogueEntry } from '@core/challenges/challenge.model';
import { ChallengeTier } from '@core/challenges/challenge-visual.model';

/**
 * One operator on the squad gauge: a hexagon lit when they validated the challenge.
 */
export interface SquadSlot {
  readonly name: string;
  readonly done: boolean;
}

/**
 * A challenge as one card shows it, whether the day's or one of the week's five.
 *
 * Everything is already worded: the card only lays it out, so the day's challenge and a weekly
 * one read as the same object with a different key line.
 */
export interface ChallengeCard {
  /**
   * CSS colour the card is lit from: the tier's accent, cyan for the day's challenge, green for
   * a closed day.
   */
  readonly tone: string;

  /**
   * What the hexagon carries: the tier's numeral, or the bolt of the daily draw (`D`).
   */
  readonly mark: ChallengeTier;

  /**
   * Key line above the name: the difficulty, or "daily challenge".
   */
  readonly kind: string;

  /**
   * Second part of the key line, for a closed day: the date and that the day is over.
   */
  readonly aside: string;
  readonly competitiveOnly: boolean;
  readonly name: string;
  readonly description: string;

  /**
   * Wounded one operator brings back by validating it.
   */
  readonly survivors: number;
  /**
   * Ranking points it pays, what the card shows while no campaign is running.
   */
  readonly rankingPoints: number;
  /**
   * True while a campaign is running: the wounded count is then what the card shows.
   */
  readonly rescueActive: boolean;
  readonly slots: readonly SquadSlot[];
  readonly doneCount: number;
}

/**
 * Where one of the seven days stands.
 */
export type DayState = 'closed' | 'now' | 'ahead';

/**
 * One cell of the seven-day strip.
 */
export interface DayCell {
  /**
   * Position in the week, Monday first.
   */
  readonly index: number;
  readonly state: DayState;

  /**
   * Short weekday, the cell's own label (`Lun`).
   */
  readonly weekday: string;

  /**
   * Day and month, beside the weekday (`4 sept.`).
   */
  readonly date: string;

  /**
   * The day's challenge as a card, or `null` when none was drawn (a day ahead, or a day the tick
   * missed).
   */
  readonly card: ChallengeCard | null;
  readonly doneCount: number;
  readonly total: number;

  /**
   * The whole sentence the cell's figure abbreviates, shown on hover and read to assistive tech.
   */
  readonly tip: string;
}

/**
 * One group of the catalogue: the daily pool, or one difficulty of the weekly one.
 */
export interface CatalogueGroup {
  readonly key: string;
  readonly tone: string;
  readonly mark: ChallengeTier;
  readonly label: string;
  readonly entries: readonly ChallengeCatalogueEntry[];
}

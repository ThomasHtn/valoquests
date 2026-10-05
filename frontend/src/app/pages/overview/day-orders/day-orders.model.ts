import type { LucideIcon } from '@lucide/angular';

/**
 * Day's gains, line by line.
 */
export interface DayTally {
  /**
   * One-based week index, shown as `Boss 04`.
   */
  readonly weekIndex: number;

  /**
   * Guardian damage dealt today.
   */
  readonly damage: number;

  /**
   * Components gained today.
   */
  readonly components: number;

  /**
   * Carry capacity gained today.
   */
  readonly carryGained: number;

  /**
   * Food gained today.
   */
  readonly food: number;

  /**
   * Shelter capacity gained today.
   */
  readonly shelterGained: number;

  /**
   * Food eaten today.
   */
  readonly upkeep: number;

  /**
   * Inhabitants tonight.
   */
  readonly population: number;

  /**
   * Inhabitants gained or lost today, net of famine and guardian losses.
   */
  readonly populationChange: number;

  /**
   * Operators who played today.
   */
  readonly presence: number;

  /**
   * Operators on the roster.
   */
  readonly roster: number;

  /**
   * One slot per operator of the roster, lit for those who played.
   */
  readonly pips: readonly DayPip[];
}

/**
 * One operator slot of the day's presence gauge.
 */
export interface DayPip {
  /**
   * Operator who played, `null` for an empty slot (absentees stay unnamed).
   */
  readonly name: string | null;

  /**
   * Portrait of the operator who played, `null` for an empty slot or none.
   */
  readonly portrait: string | null;

  /**
   * Whether the slot is lit.
   */
  readonly on: boolean;
}

/**
 * Tone of a tally tile: its resource, the base growing or shrinking, or a cost.
 */
export type TallyTileTone = 'components' | 'food' | 'growth' | 'decline' | 'cost';

/**
 * One base flow of the day, every text already translated.
 */
export interface TallyTile {
  /**
   * Colour of the tile, unique per tile.
   */
  readonly tone: TallyTileTone;

  /**
   * Icon of the resource.
   */
  readonly icon: LucideIcon;

  /**
   * Name of the flow.
   */
  readonly label: string;

  /**
   * Tooltip explaining the flow.
   */
  readonly tooltip: string;

  /**
   * Signed amount of the day.
   */
  readonly figure: string;

  /**
   * Capacity the amount bought, shown as a chip; `null` for a plain note.
   */
  readonly gain: string | null;

  /**
   * Caption after the amount when there is no chip.
   */
  readonly note: string | null;
}

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

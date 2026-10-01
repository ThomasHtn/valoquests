/**
 * A season code split into the two parts a compact label shows apart.
 */
export interface SeasonParts {
  /**
   * The era the act belongs to, spelled out, e.g. `Ép. 11` or `2026`.
   */
  readonly era: string;

  /**
   * The era as a short badge, e.g. `É11` or `2026`.
   */
  readonly eraMark: string;

  /**
   * Number of the act within that era.
   */
  readonly act: number;
}

/**
 * Season code split for a compact label.
 */
export interface SeasonParts {
  /**
   * Era spelled out, e.g. `Ép. 11` or `2026`.
   */
  readonly era: string;

  /**
   * Era as a short badge, e.g. `É11` or `2026`.
   */
  readonly eraMark: string;

  /**
   * Act number within the era.
   */
  readonly act: number;
}

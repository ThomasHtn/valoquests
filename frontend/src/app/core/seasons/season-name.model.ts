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

/**
 * Raw season code read into its era and act.
 */
export interface SeasonCode {
  /**
   * Naming era: numbered episodes (`e10a3`) or years (`v26a4`).
   */
  readonly era: 'episode' | 'year';

  /**
   * Episode number, or the full year (`2026`).
   */
  readonly number: number;

  /**
   * Act number within the era.
   */
  readonly act: number;
}

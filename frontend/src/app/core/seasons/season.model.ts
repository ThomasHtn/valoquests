/**
 * Season filtering match history, mirrors the backend `SeasonResponse`.
 */
export interface Season {
  /**
   * Internal id.
   */
  readonly id: number;

  /**
   * Raw season code, e.g. `e9a2`.
   */
  readonly name: string;

  /**
   * Whether this season is in progress.
   */
  readonly active: boolean;
}

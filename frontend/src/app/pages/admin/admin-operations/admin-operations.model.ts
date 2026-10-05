/**
 * One counter of the latest synchronization run.
 */
export interface SynchronizationFigure {
  /**
   * Translation key of the counter's caption.
   */
  readonly labelKey: string;

  /**
   * Counter value.
   */
  readonly value: number;

  /**
   * Whether the value calls for attention (danger tone).
   */
  readonly alert: boolean;
}

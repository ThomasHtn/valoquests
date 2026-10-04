/**
 * One option of an `app-select`.
 */
export interface SelectOption<T> {
  /**
   * Value emitted when this option is chosen.
   */
  readonly value: T;

  /**
   * Translated label.
   */
  readonly label: string;
}

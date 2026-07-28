/**
 * A selectable option in an {@link Select}, generic over the value's domain type.
 */
export interface SelectOption<T> {
  /**
   * Value emitted when this option is chosen.
   */
  readonly value: T;

  /**
   * Already-translated label shown for this option.
   */
  readonly label: string;
}

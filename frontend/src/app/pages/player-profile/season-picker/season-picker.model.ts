/**
 * One season as the picker lists it.
 */
export interface SeasonPickerOption {
  /**
   * Season id.
   */
  readonly id: number;

  /**
   * Era badge such as `É11`, `null` when no known era matches.
   */
  readonly mark: string | null;

  /**
   * Label beside the badge: the act, or the raw code without a known era.
   */
  readonly label: string;

  /**
   * Full season name, for assistive technology.
   */
  readonly fullName: string;

  /**
   * Era key, so the list can rule off one episode from the next.
   */
  readonly era: string;
}

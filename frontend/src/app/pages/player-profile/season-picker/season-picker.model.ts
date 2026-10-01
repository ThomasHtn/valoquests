/**
 * One season as the picker lists it.
 */
export interface SeasonPickerOption {
  /**
   * Identifier of the season.
   */
  readonly id: number;

  /**
   * Short badge naming the era, e.g. `É11`, or `null` for a code no known era matches.
   */
  readonly mark: string | null;

  /**
   * Label beside the badge: the act, or the raw code when no known era matches.
   */
  readonly label: string;

  /**
   * Full season name, read out to assistive technology.
   */
  readonly fullName: string;

  /**
   * Era key, so the list can rule off one episode from the next.
   */
  readonly era: string;
}

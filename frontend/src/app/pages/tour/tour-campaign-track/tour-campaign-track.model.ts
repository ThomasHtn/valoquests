/**
 * Where a week of the campaign stands against the current one.
 */
export type TourTrackState = 'done' | 'now' | 'ahead';

/**
 * One planet of the campaign track.
 */
export interface TourTrackPlanet {
  /**
   * One-based index of the week in the campaign.
   */
  readonly weekIndex: number;

  /**
   * Public path of the planet's drawing.
   */
  readonly art: string;

  /**
   * Whether the week is evacuated, under way or still to come.
   */
  readonly state: TourTrackState;

  /**
   * Name under the planet: the planet's own for the current week, its two-digit rank otherwise.
   */
  readonly label: string;
}

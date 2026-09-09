/**
 * One zone of the target dummy, with the share of hits it took.
 */
export interface AimZone {
  /**
   * Translation key suffix naming the zone.
   */
  readonly key: 'head' | 'body' | 'legs';

  /**
   * Share of registered hits that landed there, as a percentage.
   */
  readonly percentage: number;

  /**
   * The share, formatted for display.
   */
  readonly label: string;

  /**
   * Fill opacity of the zone on the silhouette, between the faintest tint and a full flat.
   */
  readonly opacity: number;
}

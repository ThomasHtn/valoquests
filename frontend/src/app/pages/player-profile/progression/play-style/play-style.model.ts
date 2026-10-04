/**
 * One zone of the target dummy, with the share of hits it took.
 */
export interface AimZone {
  /**
   * Translation key suffix naming the zone.
   */
  readonly key: 'head' | 'body' | 'legs';

  /**
   * Share of registered hits that landed there, in percent.
   */
  readonly percentage: number;

  /**
   * Formatted share.
   */
  readonly label: string;

  /**
   * Fill opacity on the silhouette, from the faintest tint to full.
   */
  readonly opacity: number;
}

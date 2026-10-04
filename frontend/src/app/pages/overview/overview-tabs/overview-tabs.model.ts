/**
 * Overview tab key.
 */
export type OverviewTabKey = 'challenges' | 'contributions' | 'matches' | 'campaign';

/**
 * One tab as the bar draws it.
 */
export interface OverviewTab {
  /**
   * Which tab.
   */
  readonly key: OverviewTabKey;

  /**
   * Translated name of the tab.
   */
  readonly label: string;
}

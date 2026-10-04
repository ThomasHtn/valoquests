/**
 * Where the context bar's way back leads.
 */
export interface BackTarget {
  /**
   * Target URL.
   */
  readonly link: string;

  /**
   * Translated name of the target page.
   */
  readonly label: string;

  /**
   * Whether it goes back through history, restoring the previous page's filters and scroll.
   */
  readonly viaHistory: boolean;
}

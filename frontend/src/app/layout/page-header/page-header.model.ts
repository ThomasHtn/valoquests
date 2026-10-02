/**
 * Where the context bar's way back leads.
 */
export interface BackTarget {
  /**
   * The URL it leads to.
   */
  readonly link: string;

  /**
   * Already-translated name of that page.
   */
  readonly label: string;

  /**
   * Whether it is the page the reader came from, reached through the browser's own history so its
   * filters and scroll come back with it.
   */
  readonly viaHistory: boolean;
}

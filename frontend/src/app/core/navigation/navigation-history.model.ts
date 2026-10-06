/**
 * One browser history entry of the app, as the history service tracks it.
 */
export interface HistoryEntry {
  /**
   * Router navigation ids stored in the entry's state, the latest last; a rewrite adds one.
   */
  readonly ids: readonly number[];

  /**
   * URL the entry shows.
   */
  readonly url: string;
}

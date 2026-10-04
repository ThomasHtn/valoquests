/**
 * Paginated API result, mirrors the backend `PageResponse<T>`.
 */
export interface PageResponse<T> {
  /**
   * Entries of the page.
   */
  readonly content: readonly T[];

  /**
   * Zero-based page index.
   */
  readonly page: number;

  /**
   * Requested page size.
   */
  readonly size: number;

  /**
   * Entries across every page.
   */
  readonly totalElements: number;

  /**
   * Number of pages.
   */
  readonly totalPages: number;
}

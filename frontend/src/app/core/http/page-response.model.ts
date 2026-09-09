/**
 * Generic, immutable representation of a paginated API result.
 *
 * Mirrors the backend `PageResponse<T>`.
 */
export interface PageResponse<T> {
  /**
   * Entries of the requested page.
   */
  readonly content: readonly T[];

  /**
   * Zero-based index of the page.
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

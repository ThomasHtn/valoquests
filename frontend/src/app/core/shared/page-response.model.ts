/**
 * Generic, immutable representation of a paginated API result.
 *
 * Mirrors the backend `PageResponse<T>`.
 */
export interface PageResponse<T> {
  readonly content: readonly T[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
}

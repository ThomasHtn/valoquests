import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { LucideChevronLeft, LucideChevronRight } from '@lucide/angular';

/**
 * Previous/next pagination controls with a page label.
 *
 * Shared by every screen paginating a resource so they all navigate and disable bounds the same
 * way. Owns the boundary clamping so callers only need to react to {@link pageChange}.
 */
@Component({
  selector: 'app-pagination',
  imports: [LucideChevronLeft, LucideChevronRight],
  templateUrl: './pagination.html',
  host: { class: 'block' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Pagination {
  /**
   * Zero-based index of the currently displayed page.
   */
  public readonly page = input.required<number>();

  /**
   * Total number of available pages.
   */
  public readonly totalPages = input.required<number>();

  /**
   * Already-translated label for the "previous" button.
   */
  public readonly previousLabel = input.required<string>();

  /**
   * Already-translated label for the "next" button.
   */
  public readonly nextLabel = input.required<string>();

  /**
   * Already-translated "page X of Y" label.
   */
  public readonly pageLabel = input.required<string>();

  /**
   * Emits the new zero-based page index whenever the user moves to the previous or next page.
   */
  public readonly pageChange = output<number>();

  /**
   * Moves to the previous page, clamped to the first page.
   */
  protected goToPreviousPage(): void {
    this.pageChange.emit(Math.max(0, this.page() - 1));
  }

  /**
   * Moves to the next page, clamped to the last page.
   */
  protected goToNextPage(): void {
    this.pageChange.emit(Math.min(this.totalPages() - 1, this.page() + 1));
  }
}

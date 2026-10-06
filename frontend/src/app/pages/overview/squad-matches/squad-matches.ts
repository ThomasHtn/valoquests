import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
  untracked,
} from '@angular/core';

import { PublicResources } from '@core/http/public-resources';
import { Translation } from '@core/i18n/translation';
import { HistoryMatch, MatchDay } from '@core/matches/day/match-day.model';
import { groupMatchesByDay } from '@core/matches/day/match-day.utils';
import { MatchesApi } from '@core/matches/matches-api';
import { EmptyPlate } from '@shared/empty-plate/empty-plate.model';
import { MatchHistory } from '@shared/match-history/match-history';

import { toHistoryMatch } from './squad-matches.utils';

/**
 * Matches tab: the roster's matches of the day, newest first, paged on scroll.
 */
@Component({
  selector: 'app-squad-matches',
  imports: [MatchHistory],
  templateUrl: './squad-matches.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SquadMatches {
  /**
   * Translation service, for the empty state and day headings.
   */
  private readonly translation = inject(Translation);

  /**
   * Shared resources, whose reloads this page-local list follows.
   */
  private readonly publicResources = inject(PublicResources);

  /**
   * Reload counter of the shared resources.
   */
  private readonly revision = this.publicResources.revision;

  /**
   * Zero-based index of the last requested page.
   */
  private readonly page = signal(0);

  /**
   * Pages folded into {@link matches}, which a failed refresh leaves as they were.
   */
  private readonly loadedPages = signal(0);

  /**
   * Page count of the last successful read, kept through a failed one.
   */
  private readonly totalPages = signal(0);

  /**
   * Current page of the roster's matches, refetched when the page moves.
   */
  protected readonly resource = inject(MatchesApi).squad(this.page);

  /**
   * Every match fetched so far; the resource holds one page.
   */
  protected readonly matches = signal<readonly HistoryMatch[]>([]);

  /**
   * Fetched matches grouped by day, for the history list.
   */
  protected readonly days = computed<readonly MatchDay<HistoryMatch>[]>(() =>
    groupMatchesByDay(this.matches(), this.translation.language()),
  );

  /**
   * Empty state shown while the roster has no match today.
   */
  protected readonly emptyPlate = computed<EmptyPlate>(() => ({
    illustration: 'matches',
    title: this.translation.translate('overview.matches.empty.title'),
    text: this.translation.translate('overview.matches.empty.text'),
    readouts: [],
  }));

  /**
   * Whether another page remains to fetch.
   */
  protected readonly hasMore = computed(() => this.loadedPages() < this.totalPages());

  /**
   * Whether a further page is loading, as opposed to the first one.
   */
  protected readonly isLoadingMore = computed(() => this.resource.isLoading() && this.page() > 0);

  /**
   * Whether a further page failed; a failed refresh of the first one keeps the list silently.
   */
  protected readonly loadMoreFailed = computed(
    () => !!this.resource.error() && this.page() > 0 && this.matches().length > 0,
  );

  constructor() {
    // The first run is the initial load, already requested by the resource.
    const initialRevision = this.revision();
    effect(() => {
      if (this.revision() === initialRevision) {
        return;
      }
      // Back to the newest page, which a day turn may also have emptied.
      if (untracked(this.page) > 0) {
        this.page.set(0);
      } else {
        this.resource.reload();
      }
    });

    // Untracked `page`, so a load starting does not re-append the previous page.
    effect(() => {
      if (this.resource.isLoading() || !this.resource.hasValue()) {
        return;
      }
      const { content, totalPages } = this.resource.value();
      const rows = content.map(toHistoryMatch);
      const page = untracked(this.page);
      if (page === 0) {
        this.matches.set(rows);
      } else {
        this.matches.update((matches) => [...matches, ...rows]);
      }
      this.loadedPages.set(page + 1);
      this.totalPages.set(totalPages);
    });

    // A failed refresh keeps the list on screen; flagged stale, the next poll tries again.
    effect(() => {
      if (
        this.resource.error() !== undefined &&
        untracked(this.page) === 0 &&
        untracked(this.matches).length > 0
      ) {
        untracked(() => this.publicResources.markStale());
      }
    });
  }

  /**
   * Requests the next page, unless none is left or one is already loading.
   */
  protected loadMore(): void {
    if (!this.hasMore() || this.resource.isLoading()) {
      return;
    }
    // From the pages on screen: after a failed refresh `page` is back at 0 while the list is not.
    this.page.set(this.loadedPages());
  }
}

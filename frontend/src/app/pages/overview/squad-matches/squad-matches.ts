import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
  untracked,
} from '@angular/core';

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
   * Zero-based index of the last requested page.
   */
  private readonly page = signal(0);

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
  protected readonly hasMore = computed(() =>
    this.resource.hasValue() ? this.page() + 1 < this.resource.value().totalPages : false,
  );

  /**
   * Whether a further page is loading, as opposed to the first one.
   */
  protected readonly isLoadingMore = computed(() => this.resource.isLoading() && this.page() > 0);

  constructor() {
    // Untracked `page`, so a load starting does not re-append the previous page.
    effect(() => {
      if (this.resource.isLoading() || !this.resource.hasValue()) {
        return;
      }
      const rows = this.resource.value().content.map(toHistoryMatch);
      if (untracked(this.page) === 0) {
        this.matches.set(rows);
      } else {
        this.matches.update((matches) => [...matches, ...rows]);
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
    this.page.update((page) => page + 1);
  }
}

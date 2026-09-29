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
import { MatchesApi } from '@core/matches/matches-api';
import { EmptyPlate } from '@shared/empty-plate/empty-plate.model';
import { HistoryMatch, MatchDay } from '../../player-profile/match-day.model';
import { groupMatchesByDay } from '../../player-profile/match-day.utils';
import { MatchHistory } from '../../player-profile/match-history/match-history';
import { toHistoryMatch } from './squad-matches.utils';

/**
 * The overview's matches tab: the day's matches of the campaign roster, newest first, drawn like a
 * profile's history with each row led by its player. Loaded a page at a time as the reader scrolls.
 */
@Component({
  selector: 'app-squad-matches',
  imports: [MatchHistory],
  templateUrl: './squad-matches.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SquadMatches {
  private readonly translation = inject(Translation);

  /**
   * Zero-based index of the last requested page.
   */
  private readonly page = signal(0);

  protected readonly resource = inject(MatchesApi).squad(this.page);

  /**
   * Every match fetched so far; the resource only ever holds one page.
   */
  protected readonly matches = signal<readonly HistoryMatch[]>([]);

  protected readonly days = computed<readonly MatchDay<HistoryMatch>[]>(() =>
    groupMatchesByDay(this.matches(), this.translation.language()),
  );

  protected readonly emptyPlate = computed<EmptyPlate>(() => ({
    illustration: 'matches',
    title: this.translation.translate('overview.matches.empty.title'),
    text: this.translation.translate('overview.matches.empty.text'),
    readouts: [],
  }));

  protected readonly hasMore = computed(() =>
    this.resource.hasValue() ? this.page() + 1 < this.resource.value().totalPages : false,
  );

  protected readonly isLoadingMore = computed(() => this.resource.isLoading() && this.page() > 0);

  constructor() {
    // Appends each settled page; `page` is read untracked so a load starting does not re-append
    // the previous page.
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

  protected loadMore(): void {
    if (!this.hasMore() || this.resource.isLoading()) {
      return;
    }
    this.page.update((page) => page + 1);
  }
}

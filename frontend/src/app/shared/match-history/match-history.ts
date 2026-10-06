import { NgTemplateOutlet } from '@angular/common';
import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  inject,
  input,
  output,
  viewChild,
} from '@angular/core';
import { RouterLink } from '@angular/router';

import { LucideArrowDown, LucideDynamicIcon } from '@lucide/angular';

import { CONCEPT_ICONS } from '@core/concepts/concept.constants';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { HistoryMatch, MatchDay } from '@core/matches/day/match-day.model';
import {
  resolveAgentImageUrl,
  resolveAgentInitial,
  resolveMapImageUrl,
} from '@core/matches/display/match-format.utils';
import { Breakpoint } from '@core/viewport/breakpoint';
import { Avatar } from '@shared/avatar/avatar';
import { EmptyPlate } from '@shared/empty-plate/empty-plate.model';
import { MediaThumbnail } from '@shared/media-thumbnail/media-thumbnail';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SKELETON_ROWS } from '@shared/resource-state/resource-state-skeleton.constants';
import { Spinner } from '@shared/spinner/spinner';
import { Tooltip } from '@shared/tooltip/tooltip';

import { STAT_TONE_CLASSES } from './match-history.constants';
import { buildMatchHistoryDays } from './match-history.utils';

/**
 * Match history grouped by day (grid on large viewports, cards below), for a player or the squad.
 * Presentational: the page owns paging and filters, this asks for the next page on scroll.
 */
@Component({
  selector: 'app-match-history',
  imports: [
    NgTemplateOutlet,
    LucideDynamicIcon,
    TranslatePipe,
    RouterLink,
    MediaThumbnail,
    ResourceState,
    Avatar,
    Tooltip,
    LucideArrowDown,
    Spinner,
  ],
  templateUrl: './match-history.html',
  styleUrl: './match-history.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MatchHistory {
  /**
   * Icon of each concept.
   */
  protected readonly concepts = CONCEPT_ICONS;

  /**
   * Player whose history this is, `null` on the squad's history.
   */
  public readonly playerId = input<number | null>(null);

  /**
   * Matches fetched so far, grouped by day.
   */
  public readonly days = input.required<readonly MatchDay<HistoryMatch>[]>();

  /**
   * Empty-state plate, `null` for the profile's filter message.
   */
  public readonly emptyPlate = input<EmptyPlate | null>(null);

  /**
   * Whether no match matched the filters.
   */
  public readonly isEmpty = input.required<boolean>();

  /**
   * Whether the first page failed to load.
   */
  public readonly isError = input.required<boolean>();

  /**
   * Whether the first page is loading; later pages use {@link isLoadingMore}.
   */
  public readonly isLoading = input.required<boolean>();

  /**
   * Whether a page past the last fetched one still exists.
   */
  public readonly hasMore = input.required<boolean>();

  /**
   * Whether a page beyond the first is being fetched.
   */
  public readonly isLoadingMore = input.required<boolean>();

  /**
   * Whether a later page failed: shown matches stay, with a retry row.
   */
  public readonly loadMoreFailed = input(false);

  /**
   * Emitted when the reader retries after an error.
   */
  public readonly retry = output<void>();

  /**
   * Emitted when the trailing sentinel scrolls into view.
   */
  public readonly loadMore = output<void>();

  /**
   * Grid layout when large, cards otherwise; only one is ever in the DOM.
   */
  protected readonly isLarge = inject(Breakpoint).isLarge;

  /**
   * Placeholder rows drawn while the first page loads.
   */
  protected readonly skeletonRows = SKELETON_ROWS;

  /**
   * Modifier of each stat tone, for the template.
   */
  protected readonly statToneClasses = STAT_TONE_CLASSES;

  /**
   * Agent initial shown when the portrait is missing, for the template.
   */
  protected readonly agentInitial = resolveAgentInitial;

  /**
   * Map thumbnail URL, for the template.
   */
  protected readonly mapImageUrl = resolveMapImageUrl;

  /**
   * Agent portrait URL, for the template.
   */
  protected readonly agentImageUrl = resolveAgentImageUrl;

  /**
   * Translation service, whose language formats the figures.
   */
  private readonly translation = inject(Translation);

  /**
   * Sentinel after the last match while {@link hasMore} holds.
   */
  private readonly loadMoreTrigger = viewChild<ElementRef<HTMLElement>>('loadMoreTrigger');

  /**
   * Days formatted for the template; recomputed when the language changes.
   */
  protected readonly historyDays = computed(() =>
    buildMatchHistoryDays(
      this.days(),
      this.playerId(),
      this.translation.language(),
      (key, params) => this.translation.translate(key, params),
    ),
  );

  constructor() {
    // After render: the sentinel only exists once `hasMore` has rendered it.
    afterRenderEffect((onCleanup) => {
      const element = this.loadMoreTrigger()?.nativeElement;
      if (!element) {
        return;
      }
      const observer = new IntersectionObserver((entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          this.loadMore.emit();
        }
      });
      observer.observe(element);
      onCleanup(() => observer.disconnect());
    });
  }
}

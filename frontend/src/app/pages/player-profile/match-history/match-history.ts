import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  input,
  output,
  viewChild,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideArrowDown, LucideLoaderCircle, LucideDynamicIcon } from '@lucide/angular';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { formatLocalTime } from '@core/date/date-time.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import {
  resolveAgentImageUrl,
  resolveAgentInitial,
  resolveDamageHintKey,
  resolveMapImageUrl,
  resolveMatchScore,
} from '@core/matches/match-format.utils';
import { resolveResultAccentClass, resolveResultTextClass } from '@core/matches/match-visual.utils';
import { Match } from '@core/matches/match.model';
import {
  formatHeadshotPercentage,
  formatKda,
  formatScore,
} from '@core/players/player-format.utils';
import { resolveKdVisual, resolveStatTextClass } from '@core/players/player-stats.utils';
import { Breakpoint } from '@core/viewport/breakpoint';
import { EmptyPlate } from '@shared/empty-plate/empty-plate.model';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SKELETON_ROWS } from '@shared/resource-state/skeleton.constants';
import { Avatar } from '@shared/avatar/avatar';
import { Tooltip } from '@shared/tooltip/tooltip';
import { HistoryMatch, MatchDay } from '../match-day.model';
import { MediaThumbnail } from '../media-thumbnail/media-thumbnail';
import { MATCH_ROW_GRID_CLASS } from '../player-profile.constants';
import { formatPercent } from '@core/i18n/number-format.utils';
import { CONCEPT_ICONS } from '@core/concepts/concept.constants';

/**
 * A match history, grouped by day: a grid on large viewports, cards below. Serves a player's own
 * history and the squad's shared one, whose rows lead with the player instead of the map.
 *
 * Presentational: the page owns the paging and the filters, this component only renders what it
 * is given and asks for the next page once its trailing sentinel scrolls into view.
 */
@Component({
  selector: 'app-match-history',
  imports: [
    LucideDynamicIcon,
    TranslatePipe,
    RouterLink,
    MediaThumbnail,
    ResourceState,
    Avatar,
    Tooltip,
    LucideArrowDown,
    LucideLoaderCircle,
  ],
  templateUrl: './match-history.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MatchHistory {
  /**
   * The one icon of each concept, read by the template's `svg[lucideIcon]`.
   */
  protected readonly concepts = CONCEPT_ICONS;

  /**
   * Player whose history this is, for the links out to each match; `null` on the squad's history,
   * where each row names its own player.
   */
  public readonly playerId = input<number | null>(null);

  /**
   * Every match fetched so far, grouped into the days they were played on.
   */
  public readonly days = input.required<readonly MatchDay<HistoryMatch>[]>();

  /**
   * Empty state drawn as a plate, for a history that waits on matches rather than on a filter to
   * clear; `null` keeps the profile's filter message.
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
   * Whether the first page is still loading; later pages report through {@link isLoadingMore}.
   */
  public readonly isLoading = input.required<boolean>();

  /**
   * Whether a page past the last fetched one still exists.
   */
  public readonly hasMore = input.required<boolean>();

  /**
   * Whether a page beyond the first is being fetched, shown as a small row under the list.
   */
  public readonly isLoadingMore = input.required<boolean>();

  /**
   * Whether the last page past the first failed: the matches already shown stay, with a retry row
   * under them.
   */
  public readonly loadMoreFailed = input(false);

  /**
   * Emitted when the reader asks to retry after an error.
   */
  public readonly retry = output<void>();

  /**
   * Emitted when the trailing sentinel scrolls into view and a next page exists.
   */
  public readonly loadMore = output<void>();

  /**
   * Whether the viewport can hold the match grid: below it, the same matches are rendered as
   * cards. Only the matching layout is put in the DOM, never both.
   */
  protected readonly isLarge = inject(Breakpoint).isLarge;

  /**

   * Shared column grid of the desktop rows, so header and rows line up.

   */
  protected readonly rowGridClass = MATCH_ROW_GRID_CLASS;

  protected readonly skeletonRows = SKELETON_ROWS;

  protected readonly resultAccentClass = resolveResultAccentClass;

  protected readonly resultTextClass = resolveResultTextClass;

  protected readonly matchTime = formatLocalTime;

  protected readonly agentInitial = resolveAgentInitial;

  protected readonly mapImageUrl = resolveMapImageUrl;

  protected readonly agentImageUrl = resolveAgentImageUrl;

  protected readonly formatKda = (kda: number | null): string =>
    formatKda(kda, this.translation.language());

  protected readonly formatHeadshotPercentage = (percentage: number | null): string =>
    formatHeadshotPercentage(percentage, this.translation.language());

  protected readonly formatScore = formatScore;

  protected readonly matchScore = resolveMatchScore;

  protected readonly kdVisual = resolveKdVisual;

  protected readonly statTextClass = resolveStatTextClass;

  private readonly translation = inject(Translation);

  /**
   * Marker rendered after the last loaded match while {@link hasMore} holds, watched by the
   * intersection observer below.
   */
  private readonly loadMoreTrigger = viewChild<ElementRef<HTMLElement>>('loadMoreTrigger');

  constructor() {
    // An after-render effect rather than a plain one: the sentinel only exists in the DOM once
    // `hasMore` has rendered it.
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

  /**
   * Route to a match's detail, under the player who played it.
   */
  protected matchLink(row: HistoryMatch): readonly (string | number)[] {
    return ['/players', row.player?.id ?? this.playerId() ?? '', 'matches', row.id];
  }

  /**
   * Formats a ValoQuests damage amount, grouped in the active language.
   */
  protected formatDamageAmount(damage: number): string {
    return formatDamage(damage, this.translation.language());
  }

  /**
   * Explains the amount a match was worth, one sentence per case the cell can show: worth nothing,
   * full value, or reduced by the day's ladder (the down arrow).
   */
  protected damageExplanation(match: Match): string {
    const percent = match.damageCoefficientPercent;
    return this.translation.translate(resolveDamageHintKey(percent), { percent });
  }

  /**
   * The reduced share a match kept, read out behind the down arrow, or `null` when the match kept
   * its full value (or none) and the arrow stays hidden.
   */
  /**
   * The kept share as the card writes it beside the arrow (`50 %`).
   */
  protected formatShare(percent: number): string {
    return formatPercent(percent, this.translation.language());
  }

  protected damageShareLabel(match: Match): string | null {
    const percent = match.damageCoefficientPercent;
    return percent <= 0 || percent >= 100
      ? null
      : this.translation.translate('playerProfile.matches.damage.share', { percent });
  }
}

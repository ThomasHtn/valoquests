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
import { formatLocalTime } from '@core/date/date-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import {
  resolveAgentImageUrl,
  resolveAgentInitial,
  resolveDamageHintKey,
  resolveMapImageUrl,
  resolveMatchScore,
} from '@core/matches/display/match-format.utils';
import {
  resolveResultAccentClass,
  resolveResultTextClass,
} from '@core/matches/display/match-visual.utils';
import { Match } from '@core/matches/match.model';
import {
  formatHeadshotPercentage,
  formatKda,
  formatScore,
} from '@core/players/player-format.utils';
import { resolveKdVisual, resolveStatTextClass } from '@core/players/stats/player-stats.utils';
import { Breakpoint } from '@core/viewport/breakpoint';
import { EmptyPlate } from '@shared/empty-plate/empty-plate.model';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SKELETON_ROWS } from '@shared/resource-state/resource-state-skeleton.constants';
import { Avatar } from '@shared/avatar/avatar';
import { Tooltip } from '@shared/tooltip/tooltip';
import { HistoryMatch, MatchDay } from '@core/matches/day/match-day.model';
import { MediaThumbnail } from '@shared/media-thumbnail/media-thumbnail';
import { MATCH_ROW_GRID_CLASS } from './match-history.constants';
import { formatPercent } from '@core/i18n/format/number-format.utils';
import { CONCEPT_ICONS } from '@core/concepts/concept.constants';

/**
 * Match history grouped by day (grid on large viewports, cards below), for a player or the squad.
 * Presentational: the page owns paging and filters, this asks for the next page on scroll.
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
   * Shared column grid of the desktop rows.
   */
  protected readonly rowGridClass = MATCH_ROW_GRID_CLASS;

  /**
   * Placeholder rows drawn while the first page loads.
   */
  protected readonly skeletonRows = SKELETON_ROWS;

  /**
   * Accent stripe class of a match result, for the template.
   */
  protected readonly resultAccentClass = resolveResultAccentClass;

  /**
   * Text color class of a match result, for the template.
   */
  protected readonly resultTextClass = resolveResultTextClass;

  /**
   * Formats a match's start as local time, for the template.
   */
  protected readonly matchTime = formatLocalTime;

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
   * Formats a combat score, for the template.
   */
  protected readonly formatScore = formatScore;

  /**
   * Split round score, so the template can colour the player's side.
   */
  protected readonly matchScore = resolveMatchScore;

  /**
   * Colours of a kill/death ratio, for the template.
   */
  protected readonly kdVisual = resolveKdVisual;

  /**
   * Text color of a stat cell, muted for the missing-value dash.
   */
  protected readonly statTextClass = resolveStatTextClass;

  /**
   * Translation service, whose language formats the figures.
   */
  private readonly translation = inject(Translation);

  /**
   * Sentinel after the last match while {@link hasMore} holds.
   */
  private readonly loadMoreTrigger = viewChild<ElementRef<HTMLElement>>('loadMoreTrigger');

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

  /**
   * Formats a KDA in the active language, bound so the template can call it.
   */
  protected readonly formatKda = (kda: number | null): string =>
    formatKda(kda, this.translation.language());

  /**
   * Formats a headshot share in the active language, bound so the template can call it.
   */
  protected readonly formatHeadshotPercentage = (percentage: number | null): string =>
    formatHeadshotPercentage(percentage, this.translation.language());

  /**
   * Route to a match's detail, under the player who played it.
   */
  protected matchLink(row: HistoryMatch): readonly (string | number)[] {
    return ['/players', row.player?.id ?? this.playerId() ?? '', 'matches', row.id];
  }

  /**
   * Damage amount, grouped in the active language.
   */
  protected formatDamageAmount(damage: number): string {
    return formatDamage(damage, this.translation.language());
  }

  /**
   * Why a match was worth its amount: nothing, full value, or reduced by the day's ladder.
   */
  protected damageExplanation(match: Match): string {
    const percent = match.damageCoefficientPercent;
    return this.translation.translate(resolveDamageHintKey(percent), { percent });
  }

  /**
   * Kept share as written beside the arrow (`50 %`).
   */
  protected formatShare(percent: number): string {
    return formatPercent(percent, this.translation.language());
  }

  /**
   * Reduced share read behind the down arrow, `null` at full value or none.
   */
  protected damageShareLabel(match: Match): string | null {
    const percent = match.damageCoefficientPercent;
    return percent <= 0 || percent >= 100
      ? null
      : this.translation.translate('playerProfile.matches.damage.share', { percent });
  }
}

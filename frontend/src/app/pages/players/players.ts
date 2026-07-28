import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideChevronRight } from '@lucide/angular';

import { TranslatePipe } from '../../core/i18n/translate-pipe';
import { Translation } from '../../core/i18n/translation';
import {
  resolveTierColorClass,
  resolveTierGroup,
  resolveTierOrdinal,
} from '../../core/players/competitive-tier.constants';
import { resolvePlayerAvatarUrl } from '../../core/players/player-avatar';
import {
  extractRiotTag,
  formatHeadshotPercentage,
  formatKda,
  formatWinRate,
} from '../../core/players/player-format.utils';
import {
  resolveKdaColorClass,
  resolveWinRateBarClass,
  resolveWinRateColorClass,
} from '../../core/players/player-stats.constants';
import { PlayerSummary } from '../../core/players/player-summary.model';
import { PlayersApi } from '../../core/players/players-api';
import { Avatar } from '../../shared/avatar/avatar';
import { ProgressBar } from '../../shared/progress-bar/progress-bar';
import { ResourceState } from '../../shared/resource-state/resource-state';
import { PlayerRow } from './players.model';

/**
 * Players list page.
 *
 * Displays every tracked player's identity, rank and statistics in a single table, sorted by
 * competitive rank so the strongest players surface first.
 */
@Component({
  selector: 'app-players',
  imports: [TranslatePipe, RouterLink, LucideChevronRight, Avatar, ProgressBar, ResourceState],
  templateUrl: './players.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Players {
  /**
   * Data-access service backing the shared players resource.
   */
  private readonly playersApi = inject(PlayersApi);

  /**
   * i18n service used to resolve each row's translated rank label.
   */
  private readonly translation = inject(Translation);

  /**
   * Reactive resource fetching every tracked player's summary.
   */
  protected readonly playersResource = this.playersApi.players;

  /**
   * Tracked players mapped to display-ready rows, sorted by competitive tier (highest first) and,
   * within the same tier, by rank rating (highest first). Tier is compared before rank rating
   * since rating resets per tier and is not otherwise comparable across tiers.
   */
  protected readonly rows = computed<readonly PlayerRow[]>(() =>
    [...this.playersResource.value()]
      .sort((a, b) => {
        const tierComparison =
          resolveTierOrdinal(b.competitiveTier) - resolveTierOrdinal(a.competitiveTier);
        return tierComparison !== 0 ? tierComparison : (b.rankRating ?? -1) - (a.rankRating ?? -1);
      })
      .map((player) => this.toRow(player)),
  );

  /**
   * Resolves the color class for a row's win rate, exposed to the template.
   */
  protected readonly winRateColorClass = resolveWinRateColorClass;

  /**
   * Resolves the progress bar fill color for a row's win rate, exposed to the template.
   */
  protected readonly winRateBarClass = resolveWinRateBarClass;

  /**
   * Resolves the color class for a row's KDA, exposed to the template.
   */
  protected readonly kdaColorClass = resolveKdaColorClass;

  /**
   * Formats a row's win rate, exposed to the template.
   */
  protected readonly formatWinRate = formatWinRate;

  /**
   * Formats a row's KDA, exposed to the template.
   */
  protected readonly formatKda = formatKda;

  /**
   * Formats a row's headshot rate, exposed to the template.
   */
  protected readonly formatHeadshotPercentage = formatHeadshotPercentage;

  /**
   * Maps a tracked player's summary to a display-ready row, resolving its avatar and translated
   * rank label.
   *
   * @param player - The tracked player's summary.
   * @returns The corresponding display-ready row.
   */
  private toRow(player: PlayerSummary): PlayerRow {
    const group = resolveTierGroup(player.competitiveTier);
    const groupLabel = this.translation.translate(`players.tiers.${group.key}`);

    return {
      id: player.id,
      displayName: player.displayName,
      tag: extractRiotTag(player.riotId),
      avatarUrl: resolvePlayerAvatarUrl(player.portrait),
      tier: {
        label: group.number ? `${groupLabel} ${group.number}` : groupLabel,
        colorClass: resolveTierColorClass(group.key),
      },
      rankRating: player.rankRating,
      winRate: player.winRate,
      kda: player.kda,
      headshotPercentage: player.headshotPercentage,
      matchesPlayed: player.matchesPlayed,
    };
  }
}

import { Component, computed, inject } from '@angular/core';

import { resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import {
  resolveCompetitiveTierIconUrl,
  resolveCompetitiveTierVisual,
  resolveTierOrdinal,
} from '@core/players/competitive-tier.utils';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { PlayerSummary } from '@core/players/player-summary.model';
import { PlayersApi } from '@core/players/players-api';
import { Avatar } from '@shared/avatar/avatar';
import { RankIconView } from '@shared/rank-icon-view/rank-icon-view';
import { ResourceState } from '@shared/resource-state/resource-state';

import { PlayerPreviewCard } from './players-preview.model';

/**
 * The squad, as shown by the guided tour's closing step.
 *
 * The players page and the profile pages are full tables built around filters and match history —
 * nothing there is small enough to drop into a tour step. This is the one visual the tour writes
 * for itself: the tracked players as a row of portraits and ranks, enough to convey "these seven
 * are the group, each has a profile" without reproducing the table.
 *
 * Reads the same shared players resource as the players page, so showing it here costs no extra
 * request once that page has been visited.
 */
@Component({
  selector: 'app-players-preview',
  imports: [TranslatePipe, Avatar, RankIconView, ResourceState],
  templateUrl: './players-preview.html',
  host: { class: 'block' },
})
export class PlayersPreview {
  /**
   * Data-access service backing the shared players resource.
   */
  private readonly playersApi = inject(PlayersApi);

  /**
   * i18n service used to resolve each card's translated rank label.
   */
  private readonly translation = inject(Translation);

  /**
   * Reactive resource fetching every tracked player's summary.
   */
  protected readonly playersResource = this.playersApi.players;

  /**
   * Tracked players as display-ready cards, sorted by competitive tier so the row opens on the
   * squad's strongest player — the same ordering as the players page, kept consistent so the tour
   * shows the list in the order the visitor will find it.
   */
  protected readonly cards = computed<readonly PlayerPreviewCard[]>(() =>
    [...resourceValue(this.playersResource, [])]
      .sort((a, b) => resolveTierOrdinal(b.competitiveTier) - resolveTierOrdinal(a.competitiveTier))
      .map((player) => this.toCard(player)),
  );

  /**
   * Whether the players resource is still loading.
   */
  protected readonly isLoading = computed(() => this.playersResource.isLoading());

  /**
   * Whether the players resource failed to load.
   */
  protected readonly hasError = computed(() => this.playersResource.error() !== undefined);

  /**
   * Reloads the players resource after a failed request.
   */
  protected reload(): void {
    this.playersResource.reload();
  }

  /**
   * Maps a tracked player's summary to a display-ready card, resolving its avatar, rank icon and
   * translated rank label.
   *
   * @param player - The tracked player's summary.
   * @returns The corresponding display-ready card.
   */
  private toCard(player: PlayerSummary): PlayerPreviewCard {
    return {
      id: player.id,
      displayName: player.displayName,
      avatarUrl: resolvePlayerAvatarUrl(player.portrait),
      rankIconUrl: resolveCompetitiveTierIconUrl(player.competitiveTier),
      tier: resolveCompetitiveTierVisual(player.competitiveTier, (key) =>
        this.translation.translate(key),
      ),
    };
  }
}

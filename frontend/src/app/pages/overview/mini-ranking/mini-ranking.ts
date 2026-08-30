import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { RankingApi } from '@core/ranking/ranking-api';
import { Avatar } from '@shared/avatar/avatar';
import { ResourceState } from '@shared/resource-state/resource-state';

/**
 * Three-row preview of the week's ranking, for the accueil's own "Classement" block.
 *
 * A link to `/leaderboard`, not a second podium: the full podium already moved there (see
 * design-review.md §3.1/§3.3 — a page-header treatment belongs on one page, and the accueil's
 * own "hero" is the town). This is a plain list, on purpose smaller than the block above it.
 */
@Component({
  selector: 'app-mini-ranking',
  imports: [TranslatePipe, RouterLink, Avatar, ResourceState],
  templateUrl: './mini-ranking.html',
  styleUrl: './mini-ranking.css',
})
export class MiniRanking {
  private readonly rankingApi = inject(RankingApi);
  private readonly translation = inject(Translation);

  protected readonly rankingResource = this.rankingApi.current;
  protected readonly isLoading = computed(() => this.rankingResource.isLoading());
  protected readonly hasError = computed(() => this.rankingResource.error() !== undefined);

  /**
   * Top 3 active entries of the week's ranking. An inactive player never consumes a position (the
   * backend omits it entirely), which is what `position != null` also filters on in `Podium`.
   */
  protected readonly top3 = computed(() =>
    (resourceValue(this.rankingResource, null)?.ranking ?? [])
      .filter((entry) => entry.position != null)
      .slice(0, 3),
  );

  protected readonly resolveAvatarUrl = resolvePlayerAvatarUrl;

  protected formatDamage(damage: number): string {
    return formatDamage(damage, this.translation.language());
  }
}

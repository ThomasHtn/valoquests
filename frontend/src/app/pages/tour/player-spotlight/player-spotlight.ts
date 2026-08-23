import { Component, computed, inject, signal } from '@angular/core';

import { anyError, anyLoading, resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import {
  resolveCompetitiveTierIconUrl,
  resolveCompetitiveTierVisual,
} from '@core/players/competitive-tier.utils';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { PersonalRecords as PersonalRecordsData } from '@core/players/player-progression.model';
import { PlayerSummary } from '@core/players/player-summary.model';
import { PlayersApi } from '@core/players/players-api';
import { PersonalRecords } from '@pages/player-profile/progression/personal-records/personal-records';
import { Avatar } from '@shared/avatar/avatar';
import { RankIconView } from '@shared/rank-icon-view/rank-icon-view';
import { ResourceState } from '@shared/resource-state/resource-state';

import { PlayerSpotlightIdentity } from './player-spotlight.model';

/**
 * Display name of the adventurer the closing step opens on.
 *
 * Hard-coded rather than picked at random, so the step is the same story every time it is walked
 * through. The roster is curated through the backoffice and this name may leave it, hence the
 * fallback below rather than a lookup that assumes it is there.
 */
const FEATURED_PLAYER_NAME = 'psilonnix';

/**
 * One adventurer and their personal bests, as shown by the guided tour's closing step.
 *
 * The step's point is that every adventurer carries a story of their own, which a row of portraits
 * could only assert — so it opens a single one instead, down to the records section of their
 * profile. The records are the profile's own `app-personal-records`, reading this player's real
 * progression: whatever figures the tour shows are the ones waiting on the profile page.
 *
 * Reads the shared players resource, so the identity costs no request of its own once any other
 * screen has loaded it; the progression behind the records is this component's only extra call.
 */
@Component({
  selector: 'app-player-spotlight',
  imports: [TranslatePipe, Avatar, RankIconView, ResourceState, PersonalRecords],
  templateUrl: './player-spotlight.html',
  host: { class: 'block' },
})
export class PlayerSpotlight {
  /**
   * Data-access service backing both the shared players resource and the progression call.
   */
  private readonly playersApi = inject(PlayersApi);

  /**
   * i18n service used to resolve the translated rank label.
   */
  private readonly translation = inject(Translation);

  /**
   * Reactive resource fetching every tracked player's summary.
   */
  private readonly playersResource = this.playersApi.players;

  /**
   * The featured player, or the first tracked one if the roster no longer holds them. `null` until
   * the players resource resolves, or if no player is tracked at all.
   */
  private readonly featured = computed<PlayerSummary | null>(() => {
    const players = resourceValue(this.playersResource, [] as readonly PlayerSummary[]);

    return (
      players.find(
        (player) => player.displayName.toLowerCase() === FEATURED_PLAYER_NAME.toLowerCase(),
      ) ??
      players[0] ??
      null
    );
  });

  /**
   * Identifier driving the progression call, `null` while the featured player is unknown — which
   * keeps that call from firing against an identifier that does not exist yet.
   */
  private readonly featuredId = computed<number | null>(() => this.featured()?.id ?? null);

  /**
   * Every season, which is what an empty selection means to the progression endpoint: the records
   * shown here are career bests, not this season's.
   */
  private readonly allSeasons = signal<readonly number[]>([]);

  /**
   * Reactive resource fetching the featured player's progression analytics.
   */
  private readonly progressionResource = this.playersApi.progression(
    this.featuredId,
    this.allSeasons,
  );

  /**
   * The featured player's identity, ready to render, or `null` while it is unknown.
   */
  protected readonly identity = computed<PlayerSpotlightIdentity | null>(() => {
    const player = this.featured();

    if (!player) {
      return null;
    }

    return {
      displayName: player.displayName,
      avatarUrl: resolvePlayerAvatarUrl(player.portrait),
      rankIconUrl: resolveCompetitiveTierIconUrl(player.competitiveTier),
      tier: resolveCompetitiveTierVisual(player.competitiveTier, (key) =>
        this.translation.translate(key),
      ),
    };
  });

  /**
   * The featured player's personal bests, or `null` while loading or on error.
   *
   * Guarded by `hasValue`: reading `value()` on a resource in an error state throws.
   */
  protected readonly records = computed<PersonalRecordsData | null>(() =>
    this.progressionResource.hasValue()
      ? (this.progressionResource.value()?.records ?? null)
      : null,
  );

  /**
   * Whether either resource is still loading.
   */
  protected readonly isLoading = anyLoading(this.playersResource, this.progressionResource);

  /**
   * Whether either resource failed to load.
   */
  protected readonly hasError = anyError(this.playersResource, this.progressionResource);

  /**
   * Reloads both resources after a failed request.
   */
  protected reload(): void {
    this.playersResource.reload();
    this.progressionResource.reload();
  }
}

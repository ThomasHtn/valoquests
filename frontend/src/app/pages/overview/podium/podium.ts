import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { RankingApi } from '@core/ranking/ranking-api';
import { resolveChampionPlayerId } from '@core/ranking/ranking-champion.utils';
import { RankingEntry } from '@core/ranking/ranking.model';
import { resolvePositionBadgeClass } from '@core/ranking/ranking-visual.utils';
import { Avatar } from '@shared/avatar/avatar';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { PositionBadge } from '@shared/position-badge/position-badge';
import { ResourceState } from '@shared/resource-state/resource-state';

/**
 * Plinth treatment per podium position, 1st to 3rd: how tall the block stands, the color of the
 * cap it is topped with, and the tint washing down from it.
 *
 * Height carries the rank as much as color does — a gold cap alone would leave the three places
 * reading as equals in a screenshot, or to anyone who cannot separate the three hues.
 *
 * Every height derives from the `--podium-base` custom property declared on the two-column
 * container in the template: the 3rd plinth *is* that base, and the rest-of-the-field list is
 * given the exact same height. Since both columns sit on the same baseline, the 4th place row and
 * the bronze cap then line up by construction, at any base value, rather than through two
 * hardcoded heights that would drift apart the moment a row's padding changes.
 */
const PODIUM_PLINTH_CLASSES: readonly string[] = [
  'h-[calc(var(--podium-base)*1.4)] border-brand-500 bg-linear-to-b from-brand-500/18 to-transparent',
  'h-[calc(var(--podium-base)*1.15)] border-text-primary/50 bg-linear-to-b from-text-primary/8 to-transparent',
  'h-(--podium-base) border-podium-bronze/80 bg-linear-to-b from-text-primary/6 to-transparent',
];

/**
 * Hero podium of the overview page: the top 3 players in hexagon-framed avatars standing on
 * plinths, echoing the hexagon position badge already used by the weekly ranking, followed by the
 * remaining tracked players as flat rows.
 *
 * Reads the same shared current-ranking resource as `Leaderboard` directly, rather than reaching
 * into that component's internals, so both stay independent and `Leaderboard` is left unchanged.
 */
@Component({
  selector: 'app-podium',
  imports: [TranslatePipe, RouterLink, Avatar, ChampionBadge, PositionBadge, ResourceState],
  templateUrl: './podium.html',
})
export class Podium {
  /**
   * Data-access service backing the shared current-ranking resource.
   */
  private readonly rankingApi = inject(RankingApi);

  /**
   * Reactive resource fetching the current week's ranking, shared with `Leaderboard` and the
   * overview header.
   */
  protected readonly rankingResource = this.rankingApi.current;

  /**
   * Id of the reigning weekly "Champion", or `null` while unknown or before any week has been
   * finalized.
   */
  protected readonly championPlayerId = computed(() =>
    resolveChampionPlayerId(resourceValue(this.rankingApi.latestFinalizedWeek, null)),
  );

  /**
   * Every active tracked player's current ranking entry, in the order returned by the backend.
   *
   * An inactive player is excluded: the podium and its preview strip are a competitive
   * leaderboard, and an inactive player never competes for a slot. The backend omits `position`
   * from the JSON entirely when null, so the parsed value is `undefined` rather than `null` -
   * `!= null` catches both.
   */
  protected readonly entries = computed<readonly RankingEntry[]>(
    () =>
      resourceValue(this.rankingResource, null)?.ranking.filter(
        (entry) => entry.position != null,
      ) ?? [],
  );

  /**
   * The top 3 ranking entries, shown as the podium spotlight.
   */
  protected readonly top3 = computed(() => this.entries().slice(0, 3));

  /**
   * Ranking entries for positions 4 to 6, shown as a compact preview strip.
   */
  protected readonly rest = computed(() => this.entries().slice(3, 6));

  /**
   * Resolves a player's avatar asset from their portrait field, exposed to the template.
   */
  protected readonly resolveAvatarUrl = resolvePlayerAvatarUrl;

  /**
   * Resolves the text color for a podium entry's position, exposed to the template.
   */
  protected readonly podiumTextAccent = resolvePositionBadgeClass;

  /**
   * Resolves the plinth treatment for a podium position.
   *
   * @param position - The entry's 1-based position, always 1 to 3 here. `null` (an inactive
   *   player) never reaches the podium, and gets no plinth.
   * @returns The Tailwind height, border and background utilities to apply to the plinth.
   */
  protected plinthClass(position: number | null): string {
    return (position === null ? undefined : PODIUM_PLINTH_CLASSES[position - 1]) ?? '';
  }

  /**
   * Formats a position as the mockup's zero-padded plate number ("01", "02", "03").
   *
   * @param position - The entry's 1-based position, or `null` for an inactive player.
   * @returns The position as a two-digit string, or an em dash when there is none.
   */
  protected paddedRank(position: number | null): string {
    return position === null ? '—' : String(position).padStart(2, '0');
  }

  /**
   * Reloads the ranking resource after a failure.
   */
  protected reload(): void {
    this.rankingResource.reload();
  }
}

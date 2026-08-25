import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { RankingApi } from '@core/ranking/ranking-api';
import { resolveChampionPlayerId } from '@core/ranking/ranking-champion.utils';
import { RankingEntry } from '@core/ranking/ranking.model';
import { resolvePositionBadgeClass } from '@core/ranking/ranking-visual.utils';
import { Avatar } from '@shared/avatar/avatar';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { PositionBadge } from '@shared/position-badge/position-badge';
import { ResourceState } from '@shared/resource-state/resource-state';
import { BLOCK_TOOLTIP_DELAY_MS, Tooltip } from '@shared/tooltip/tooltip';

/**
 * Plinth treatment per podium position, 1st to 3rd: how tall the block stands, the color of the
 * cap it is topped with, and the tint washing down from it.
 *
 * Height carries the rank as much as color does — a gold cap alone would leave the three places
 * reading as equals in a screenshot, or to anyone who cannot separate the three hues.
 *
 * Every height derives from the `--podium-base` custom property declared on the two-column
 * container in the template: the 3rd plinth *is* that base, and the other two are multiples of it.
 * So the three stay in proportion at any base value, rather than through three hardcoded heights
 * that would drift apart the moment a row's padding changes. The rest-of-the-field list beside
 * them takes its height from the grid row the plinths set, and needs no measure of its own.
 */
const PODIUM_PLINTH_CLASSES: readonly string[] = [
  'h-[calc(var(--podium-base)*1.4)] border-brand-500 bg-linear-to-b from-brand-500/18 to-transparent',
  'h-[calc(var(--podium-base)*1.15)] border-text-primary/50 bg-linear-to-b from-text-primary/8 to-transparent',
  'h-(--podium-base) border-podium-bronze/80 bg-linear-to-b from-text-primary/6 to-transparent',
];

/**
 * Hero podium of the overview page: the top 3 players in circular avatars standing on
 * plinths, followed by the remaining tracked players as flat rows.
 *
 * Reads the same shared current-ranking resource as `Leaderboard` directly, rather than reaching
 * into that component's internals, so both stay independent and `Leaderboard` is left unchanged.
 */
@Component({
  selector: 'app-podium',
  imports: [
    TranslatePipe,
    RouterLink,
    Avatar,
    ChampionBadge,
    PositionBadge,
    ResourceState,
    Tooltip,
  ],
  templateUrl: './podium.html',
  // Transparent host: the section itself becomes the grid item of the overview's two-column row,
  // so it stretches to the row's height and can align its bottom edge with the panel beside it.
  host: { class: 'contents' },
})
export class Podium {
  /**
   * Data-access service backing the shared current-ranking resource.
   */
  private readonly rankingApi = inject(RankingApi);

  /**
   * How long the pointer rests on the block before its tooltip opens.
   */
  protected readonly tooltipDelayMs = BLOCK_TOOLTIP_DELAY_MS;

  /**
   * i18n service read for the active language when grouping damage amounts.
   */
  private readonly translation = inject(Translation);

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
   * Every ranking entry below the podium, shown as a compact strip beside it.
   *
   * Not capped: the overview is the only screen a visitor is guaranteed to open, and a squad
   * member silently missing from it — with no "see the full ranking" affordance anywhere on the
   * page — reads as a bug rather than as a summary.
   */
  protected readonly rest = computed(() => this.entries().slice(3));

  /**
   * Resolves a player's avatar asset from their portrait field, exposed to the template.
   */
  protected readonly resolveAvatarUrl = resolvePlayerAvatarUrl;

  /**
   * Resolves the text color for a podium entry's position, exposed to the template.
   */
  protected readonly podiumTextAccent = resolvePositionBadgeClass;

  /**
   * Groups a damage amount in the active language (`"12 400"`).
   *
   * The ranking table and the campaign timeline both print these figures grouped; the podium is
   * the same number on another screen and has no reason to print it raw.
   *
   * @param damage - The raw damage amount.
   * @returns The grouped amount.
   */
  protected formatDamage(damage: number): string {
    return formatDamage(damage, this.translation.language());
  }

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

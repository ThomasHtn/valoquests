import { Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { formatWeekdayDayMonth } from '@core/date/date-time.utils';
import { resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { PlayersApi } from '@core/players/players-api';
import { RankingApi } from '@core/ranking/ranking-api';
import { Avatar } from '@shared/avatar/avatar';
import { PositionBadge } from '@shared/position-badge/position-badge';
import { ResourceState } from '@shared/resource-state/resource-state';

/**
 * Stretch of time a preview ranks on — the two the accueil shows side by side.
 */
export type MiniRankingScope = 'DAY' | 'WEEK';

/**
 * One row of a preview, in the one shape both scopes render through.
 */
export interface MiniRankingRow {
  readonly playerId: number;
  readonly position: number | null;
  readonly displayName: string;
  readonly avatarUrl: string | null;

  /**
   * What the row is ranked on: the week's total, or the day's match damage.
   */
  readonly damage: number;
  readonly damageLabel: string;

  /**
   * Day scope only: the gap with the same figure yesterday, `null` on the week — nothing on the
   * weekly board is measured against the week before.
   */
  readonly variation: number | null;

  /**
   * {@link variation} without its sign, grouped like every other damage figure — the template
   * writes the sign itself, so the two directions share one glyph set.
   */
  readonly damageVariationLabel: string | null;

  /**
   * The same gap spelled out for assistive technology, `null` when {@link variation} is.
   */
  readonly variationLabel: string | null;
}

/**
 * Three-row preview of a ranking, for the accueil's own two "Classement" blocks.
 *
 * A link to `/leaderboard`, not a second podium: the full podium already moved there (see
 * design-review.md §3.1/§3.3 — a page-header treatment belongs on one page, and the accueil's
 * own "hero" is the town). This is a plain list, on purpose smaller than the block above it.
 *
 * One component for both scopes rather than two: the rows are the same furniture, and the scopes
 * differ only in what a figure means. The week carries the total the competition is settled on,
 * bonuses included; the day carries tonight's match damage and the gap with last night, because
 * that is all a day honestly holds — challenges and both bonuses are awarded on the week.
 *
 * It carries no frame of its own: the page names it with a section rule, the same way the profile's
 * match history is a stack of rows on the page rather than a panel.
 */
@Component({
  selector: 'app-mini-ranking',
  imports: [TranslatePipe, RouterLink, Avatar, PositionBadge, ResourceState],
  templateUrl: './mini-ranking.html',
  styleUrl: './mini-ranking.css',
  // Fills whatever height the page gives it, so the three rows can share the column's spare space
  // and end level with the block beside them.
  host: { class: 'flex min-h-0 flex-1 flex-col' },
})
export class MiniRanking {
  private readonly rankingApi = inject(RankingApi);
  private readonly playersApi = inject(PlayersApi);
  private readonly translation = inject(Translation);

  /**
   * Which board this preview reads.
   */
  public readonly scope = input.required<MiniRankingScope>();

  /**
   * The scope's own segment of the i18n tree, for the keys that differ between the two boards.
   */
  protected readonly scopeKey = computed(() => (this.scope() === 'DAY' ? 'day' : 'week'));

  private readonly weekResource = this.rankingApi.current;
  private readonly dayResource = this.rankingApi.daily;

  /**
   * The resource backing the active scope — the only one this instance waits on.
   */
  private readonly resource = computed(() =>
    this.scope() === 'DAY' ? this.dayResource : this.weekResource,
  );

  protected readonly isLoading = computed(() => this.resource().isLoading());
  protected readonly hasError = computed(() => this.resource().error() !== undefined);

  /**
   * Top 3 of the active board. An inactive player never consumes a position (the backend omits it
   * entirely), which is what `position != null` also filters on in `Podium`.
   */
  protected readonly top3 = computed<readonly MiniRankingRow[]>(() =>
    this.scope() === 'DAY' ? this.dayRows() : this.weekRows(),
  );

  /**
   * Whether the board has nothing to report yet.
   *
   * This is not the same as having no rows: the backend ranks the whole roster from the first
   * minute of the stretch, every one of them at zero until somebody plays. Three zeros are not a
   * ranking, so the block waits instead of pretending the evening has started.
   */
  protected readonly isEmpty = computed(
    () => this.top3().length === 0 || this.top3().every((row) => row.damage === 0),
  );

  /**
   * The day on the board, spelled out (`"Mardi 01/09"`), or `""` while it has not loaded.
   *
   * The backend's own day, not one computed here: it is resolved against the rollover timezone, and
   * a reader whose clock has already crossed midnight must still be told which evening they are
   * looking at.
   */
  protected readonly dayLabel = computed<string>(() => {
    const daily = resourceValue(this.dayResource, null);

    return daily == null ? '' : formatWeekdayDayMonth(daily.day, this.translation.language());
  });

  /**
   * The week's top 3, ranked on the total the competition is settled on.
   */
  private readonly weekRows = computed<readonly MiniRankingRow[]>(() =>
    (resourceValue(this.weekResource, null)?.ranking ?? [])
      .filter((entry) => entry.position != null)
      .slice(0, 3)
      .map((entry) => ({
        playerId: entry.player.id,
        position: entry.position,
        displayName: entry.player.displayName,
        avatarUrl: resolvePlayerAvatarUrl(entry.player.portrait),
        damage: entry.totalDamage,
        damageLabel: this.formatDamage(entry.totalDamage),
        variation: null,
        damageVariationLabel: null,
        variationLabel: null,
      })),
  );

  /**
   * The day's top 3, each row carrying the gap with the same player's evening yesterday.
   */
  private readonly dayRows = computed<readonly MiniRankingRow[]>(() => {
    const portraits = new Map(
      this.playersApi.players.value().map((player) => [player.id, player.portrait]),
    );

    return (resourceValue(this.dayResource, null)?.ranking ?? [])
      .filter((entry) => entry.position != null)
      .slice(0, 3)
      .map((entry) => ({
        playerId: entry.playerId,
        position: entry.position,
        displayName: entry.displayName,
        // Same fallback as the leaderboard's day board: the daily entry carries the portrait
        // stored on the match, and the roster is what fills it in when that is still empty.
        avatarUrl: resolvePlayerAvatarUrl(entry.portrait ?? portraits.get(entry.playerId) ?? null),
        damage: entry.matchDamage,
        damageLabel: this.formatDamage(entry.matchDamage),
        variation: entry.damageVariation,
        damageVariationLabel: this.formatDamage(Math.abs(entry.damageVariation)),
        variationLabel: this.variationLabel(entry.damageVariation),
      }));
  });

  private formatDamage(damage: number): string {
    return formatDamage(damage, this.translation.language());
  }

  /**
   * Spells out a day's gap with the day before, for the sr-only text beside the signed figure.
   *
   * `null` for an evening that matched the one before: that row writes its own sentence out in
   * full, so a second one would be read twice.
   */
  private variationLabel(variation: number): string | null {
    if (variation === 0) {
      return null;
    }

    const key = variation > 0 ? 'up' : 'down';

    return this.translation.translate(`overview.miniRanking.day.variation.${key}`, {
      count: this.formatDamage(Math.abs(variation)),
    });
  }
}

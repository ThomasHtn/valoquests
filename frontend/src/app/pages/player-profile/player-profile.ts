import { Component, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LucideActivity,
  LucideChevronRight,
  LucideCrosshair,
  LucideGamepad2,
  LucideSwords,
  LucideTarget,
  LucideZap,
} from '@lucide/angular';

import { resolveResultBorderClass, resolveResultTextClass } from '@core/matches/match-visual.utils';
import { Match } from '@core/matches/match.model';
import { MatchResult } from '@core/matches/match-result.model';
import { MatchesApi } from '@core/matches/matches-api';
import { SeasonsApi } from '@core/matches/seasons-api';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resolveCompetitiveTierVisual } from '@core/players/competitive-tier.utils';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import {
  extractRiotTag,
  formatHeadshotPercentage,
  formatKda,
  formatScore,
  formatWinRate,
} from '@core/players/player-format.utils';
import { resolveKdaVisual, resolveWinRateVisual } from '@core/players/player-stats.utils';
import { PlayersApi } from '@core/players/players-api';
import { Avatar } from '@shared/avatar/avatar';
import { Pagination } from '@shared/pagination/pagination';
import { ResourceState } from '@shared/resource-state/resource-state';
import { Select } from '@shared/select/select';
import { SelectOption } from '@shared/select/select.model';

/**
 * Player-profile page.
 *
 * Displays one tracked player's identity, current competitive rank, aggregated statistics and
 * filterable, paginated match history.
 */
@Component({
  selector: 'app-player-profile',
  imports: [
    TranslatePipe,
    RouterLink,
    Avatar,
    Pagination,
    ResourceState,
    Select,
    LucideActivity,
    LucideChevronRight,
    LucideCrosshair,
    LucideGamepad2,
    LucideSwords,
    LucideTarget,
    LucideZap,
  ],
  templateUrl: './player-profile.html',
})
export class PlayerProfile {
  /**
   * Internal player identifier, bound from the `:id` route parameter.
   */
  public readonly id = input.required<string>();

  /**
   * Data-access service backing the player-details resource.
   */
  private readonly playersApi = inject(PlayersApi);

  /**
   * Data-access service backing the match-history resource.
   */
  private readonly matchesApi = inject(MatchesApi);

  /**
   * Data-access service backing the shared seasons resource, used by the season filter.
   */
  private readonly seasonsApi = inject(SeasonsApi);

  /**
   * i18n service used to resolve the player's translated rank label.
   */
  private readonly translation = inject(Translation);

  /**
   * Numeric form of {@link id}, as required by the backing resources.
   */
  protected readonly playerId = computed(() => Number(this.id()));

  /**
   * Reactive resource fetching the requested player's detailed profile.
   */
  protected readonly detailsResource = this.playersApi.details(this.playerId);

  /**
   * Requested player's detailed profile, or `null` while loading or on error.
   *
   * Guarded by {@link HttpResourceRef.hasValue}: reading `value()` while the resource is in an
   * error state throws, so it must never be called unconditionally.
   */
  protected readonly details = computed(() =>
    this.detailsResource.hasValue() ? this.detailsResource.value() : null,
  );

  /**
   * Zero-based index of the requested page of match history.
   */
  protected readonly page = signal(0);

  /**
   * Selected match-result filter, or `null` to include every result.
   */
  protected readonly resultFilter = signal<MatchResult | null>(null);

  /**
   * Selected season filter, or `null` to include every season.
   */
  protected readonly seasonId = signal<number | null>(null);

  /**
   * Reactive resource fetching the requested page of match history.
   */
  protected readonly matchesResource = this.matchesApi.history(
    this.playerId,
    this.page,
    this.resultFilter,
    this.seasonId,
  );

  /**
   * Reactive resource fetching every known season, used by the season filter.
   */
  protected readonly seasonsResource = this.seasonsApi.seasons;

  /**
   * Known seasons for the season filter, or an empty list while loading or on error.
   *
   * Guarded by {@link HttpResourceRef.hasValue}: reading `value()` while the resource is in an
   * error state throws, so it must never be called unconditionally.
   */
  protected readonly seasons = computed(() =>
    this.seasonsResource.hasValue() ? this.seasonsResource.value() : [],
  );

  /**
   * Options offered by the result filter, including the "all results" entry.
   */
  protected readonly resultFilterOptions = computed<readonly SelectOption<MatchResult | null>[]>(
    () => [
      { value: null, label: this.translation.translate('playerProfile.filters.allResults') },
      { value: 'WIN', label: this.translation.translate('playerProfile.filters.win') },
      { value: 'LOSS', label: this.translation.translate('playerProfile.filters.loss') },
    ],
  );

  /**
   * Options offered by the season filter, including the "all seasons" entry.
   */
  protected readonly seasonFilterOptions = computed<readonly SelectOption<number | null>[]>(() => [
    { value: null, label: this.translation.translate('playerProfile.filters.allSeasons') },
    ...this.seasons().map((season) => ({ value: season.id, label: season.name })),
  ]);

  /**
   * Requested page's matches, or an empty list while loading or on error.
   *
   * Guarded by {@link HttpResourceRef.hasValue}: reading `value()` while the resource is in an
   * error state throws, so it must never be called unconditionally.
   */
  protected readonly rows = computed<readonly Match[]>(() =>
    this.matchesResource.hasValue() ? this.matchesResource.value().content : [],
  );

  /**
   * Total number of available pages of match history for the current filters.
   */
  protected readonly totalPages = computed(() =>
    this.matchesResource.hasValue() ? this.matchesResource.value().totalPages : 0,
  );

  /**
   * Tag segment of the player's Riot ID, or `null` when absent or not yet loaded.
   */
  protected readonly tag = computed(() => {
    const details = this.details();
    return details ? extractRiotTag(details.riotId) : null;
  });

  /**
   * Resolved avatar URL for the player's associated agent, or `null` when unavailable.
   */
  protected readonly avatarUrl = computed(() =>
    resolvePlayerAvatarUrl(this.details()?.portrait ?? null),
  );

  /**
   * Translated label and color class for the player's current competitive rank, or `null` while
   * loading.
   */
  protected readonly tier = computed(() => {
    const details = this.details();
    if (!details) {
      return null;
    }

    return resolveCompetitiveTierVisual(details.competitiveTier, (key) =>
      this.translation.translate(key),
    );
  });

  /**
   * Resolves the colors for the win-rate stat tile, exposed to the template.
   */
  protected readonly winRateVisual = resolveWinRateVisual;

  /**
   * Resolves the colors for the K/D stat tile and match rows, exposed to the template.
   */
  protected readonly kdaVisual = resolveKdaVisual;

  /**
   * Resolves a match row's left border color, exposed to the template.
   */
  protected readonly resultBorderClass = resolveResultBorderClass;

  /**
   * Resolves a match row's result text color, exposed to the template.
   */
  protected readonly resultTextClass = resolveResultTextClass;

  /**
   * Formats a win rate, exposed to the template.
   */
  protected readonly formatWinRate = formatWinRate;

  /**
   * Formats a KDA ratio, exposed to the template.
   */
  protected readonly formatKda = formatKda;

  /**
   * Formats a headshot rate, exposed to the template.
   */
  protected readonly formatHeadshotPercentage = formatHeadshotPercentage;

  /**
   * Formats an average score (ADR or ACS), exposed to the template.
   */
  protected readonly formatScore = formatScore;

  /**
   * Applies the selected result filter and resets to the first page.
   *
   * @param result - The newly selected result filter, or `null` for every result.
   */
  protected onResultFilterChange(result: MatchResult | null): void {
    this.resultFilter.set(result);
    this.page.set(0);
  }

  /**
   * Applies the selected season filter and resets to the first page.
   *
   * @param seasonId - The newly selected season id, or `null` for every season.
   */
  protected onSeasonFilterChange(seasonId: number | null): void {
    this.seasonId.set(seasonId);
    this.page.set(0);
  }
}

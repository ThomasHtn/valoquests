import { Component, computed, inject, input } from '@angular/core';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import {
  resolveAgentImageUrl,
  resolveAgentInitial,
  resolveMapImageUrl,
} from '@core/matches/match-format.utils';
import { PlayersApi } from '@core/players/players-api';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SKELETON_ROWS } from '@shared/resource-state/skeleton.constants';
import { EntityStats, EntityStatsRow } from './entity-stats/entity-stats';
import { EvolutionChart } from './evolution-chart/evolution-chart';
import { PersonalRecords } from './personal-records/personal-records';
import { PlayStyle } from './play-style/play-style';
import { SchedulePerformance } from './schedule-performance/schedule-performance';

/**
 * The player profile's progression view: how one player has been trending across the seasons they
 * selected, rather than what they did in their last ten matches.
 *
 * Fetches one payload for the whole view. Every section reads the same filtered set of matches, so
 * six endpoints would mean six round trips and six re-aggregations of the same history.
 */
@Component({
  selector: 'app-progression',
  imports: [
    TranslatePipe,
    ResourceState,
    EvolutionChart,
    PlayStyle,
    SchedulePerformance,
    PersonalRecords,
    EntityStats,
  ],
  templateUrl: './progression.html',
})
export class Progression {
  /**
   * Internal identifier of the player being profiled.
   */
  public readonly playerId = input.required<number>();

  /**
   * Seasons the analytics are scoped to; empty covers every season.
   */
  public readonly seasonIds = input.required<readonly number[]>();

  /**
   * Every known season's identifier, newest first, backing the stable per-season colors.
   */
  public readonly seasonOrder = input.required<readonly number[]>();

  /**
   * Data-access service backing the progression resource.
   */
  private readonly playersApi = inject(PlayersApi);

  /**
   * i18n service, used for the tooltips captioning each table.
   */
  private readonly translation = inject(Translation);

  /**
   * Reactive resource fetching the analytics for the current player and season selection.
   */
  protected readonly progressionResource = this.playersApi.progression(
    this.playerId,
    this.seasonIds,
  );

  /**
   * The analytics, or `null` while loading or on error.
   *
   * Guarded by `hasValue`: reading `value()` while the resource is in an error state throws, so it
   * must never be called unconditionally.
   */
  protected readonly progression = computed(() =>
    this.progressionResource.hasValue() ? this.progressionResource.value() : null,
  );

  /**
   * Whether the selection holds no competitive match at all, in which case every section below
   * would be an empty frame and the view says so once instead.
   */
  protected readonly isEmpty = computed(() => {
    const progression = this.progression();
    return progression !== null && progression.evolution.length === 0;
  });

  /**
   * Map rows, most-played first, with their images resolved.
   */
  protected readonly mapRows = computed<readonly EntityStatsRow[]>(
    () =>
      this.progression()?.maps.map((map) => ({
        name: map.mapName,
        imageUrl: resolveMapImageUrl(map.mapName),
        monogram: map.mapName.charAt(0).toUpperCase(),
        matchesPlayed: map.matchesPlayed,
        winRate: map.winRate,
        acs: map.acs,
      })) ?? [],
  );

  /**
   * Agent rows, most-played first, with their portraits resolved.
   */
  protected readonly agentRows = computed<readonly EntityStatsRow[]>(
    () =>
      this.progression()?.agents.map((agent) => ({
        name: agent.agentName,
        imageUrl: resolveAgentImageUrl(agent.agentName),
        monogram: resolveAgentInitial(agent.agentName),
        matchesPlayed: agent.matchesPlayed,
        winRate: agent.winRate,
        acs: agent.acs,
      })) ?? [],
  );

  /**
   * Placeholder line widths driving the loading skeleton.
   */
  protected readonly skeletonRows = SKELETON_ROWS;

  /**
   * Resolves a translation, exposed to the template for the tables' already-translated inputs.
   *
   * @param key - Translation key.
   * @returns The translated string.
   */
  protected translate(key: string): string {
    return this.translation.translate(key);
  }
}

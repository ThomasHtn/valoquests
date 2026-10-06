import { Component, computed, inject, input } from '@angular/core';

import { resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import {
  resolveAgentImageUrl,
  resolveAgentInitial,
  resolveMapImageUrl,
} from '@core/matches/display/match-format.utils';
import { PlayersApi } from '@core/players/players-api';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SKELETON_ROWS } from '@shared/resource-state/resource-state-skeleton.constants';

import { Consistency } from './consistency/consistency';
import { EntityStats } from './entity-stats/entity-stats';
import { EntityStatsRow } from './entity-stats/entity-stats.model';
import { EvolutionChart } from './evolution-chart/evolution-chart';
import { PersonalRecords } from './personal-records/personal-records';
import { PlayStyle } from './play-style/play-style';
import { RankJourney } from './rank-journey/rank-journey';
import { SchedulePerformance } from './schedule-performance/schedule-performance';

/**
 * Progression view: one player's trends across the selected seasons, from a single payload.
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
    RankJourney,
    Consistency,
  ],
  templateUrl: './progression.html',
  styleUrl: './progression.scss',
})
export class Progression {
  /**
   * Profiled player id.
   */
  public readonly playerId = input.required<number>();

  /**
   * Seasons in scope; empty covers every season.
   */
  public readonly seasonIds = input.required<readonly number[]>();

  /**
   * Every season id, newest first, for stable per-season colours.
   */
  public readonly seasonOrder = input.required<readonly number[]>();

  /**
   * Progression data access.
   */
  private readonly playersApi = inject(PlayersApi);

  /**
   * Analytics of the current player and seasons.
   */
  protected readonly progressionResource = this.playersApi.progression(
    this.playerId,
    this.seasonIds,
  );

  /**
   * Analytics, `null` while loading or on error (`value()` throws on error).
   */
  protected readonly progression = computed(
    () => resourceValue(this.progressionResource, undefined) ?? null,
  );

  /**
   * Whether the selection holds no competitive match, shown once instead of empty sections.
   */
  protected readonly isEmpty = computed(() => {
    const progression = this.progression();
    return progression !== null && progression.evolution.length === 0;
  });

  /**
   * Map rows, most played first, with their images.
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
   * Agent rows, most played first, with their portraits.
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
   * Placeholder line widths of the loading skeleton.
   */
  protected readonly skeletonRows = SKELETON_ROWS;
}

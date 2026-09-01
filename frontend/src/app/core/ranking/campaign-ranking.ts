import { computed, inject, Service } from '@angular/core';

import { BossApi } from '@core/boss/boss-api';
import { bossDamageOf } from '@core/boss/boss-damage.utils';
import { addDays } from '@core/date/week-period.utils';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { RankingApi } from '@core/ranking/ranking-api';

/**
 * One player's whole run, folded into a single line.
 *
 * Every figure is a sum over the weeks of the run that have been played so far, which is why none of
 * them is nullable the way a week's own are: a player who never showed up is a real zero here, not an
 * absence of data.
 */
export interface CampaignRankingEntry {
  /**
   * 1-based rank over the run, or `null` for a player who holds no ranking slot.
   */
  readonly position: number | null;
  readonly playerId: number;
  readonly displayName: string;

  /**
   * Total the run is ordered on: every week's total damage, bonuses included.
   */
  readonly totalDamage: number;

  /**
   * The share of {@link totalDamage} that came from the regularity and team bonuses.
   */
  readonly bonus: number;
  readonly completedChallenges: number;
  readonly activeDays: number;

  /**
   * Weeks of the run this player put damage into the boss.
   *
   * The campaign's own answer to the day board's "gap with yesterday": at this scale the question is
   * not how hard one evening went but how many of the run's fights somebody actually turned up for.
   * Counted on boss damage rather than on the weekly total, so a week carried entirely by the
   * regularity bonus — which never reaches the shared health bar — is not counted as a fight joined.
   */
  readonly bossCount: number;
}

/**
 * The ten-week run in progress, read as a ranking rather than as a map.
 *
 * No endpoint of its own, and deliberately: a run is a stretch of weeks, the weekly rankings are
 * already fetched in full by the leaderboard and the campaign map, and summing them here costs one
 * pass over data the application is holding anyway. What it must not do is invent a week boundary —
 * the run's first Monday is derived from the active fight's own `runWeekIndex`, which is the same
 * anchor `BossCampaign` lays its hexagons on.
 *
 * The bounds come from the boss rather than from the boss *history*: a week of the run that closed
 * without a fight leaves a hole in that history, and counting fights would make the run start on the
 * wrong Monday every time one did.
 */
@Service()
export class CampaignRanking {
  /**
   * Data-access service backing the active week's boss, read for the run's calendar bounds alone.
   */
  private readonly bossApi = inject(BossApi);

  /**
   * Data-access service backing the weekly rankings the run is summed from.
   */
  private readonly rankingApi = inject(RankingApi);

  /**
   * Whether any backing resource is still loading.
   */
  public readonly isLoading = anyLoading(
    this.bossApi.current,
    this.rankingApi.current,
    this.rankingApi.history,
  );

  /**
   * Whether any backing resource failed to load.
   */
  public readonly hasError = anyError(
    this.bossApi.current,
    this.rankingApi.current,
    this.rankingApi.history,
  );

  /**
   * Sequential number of the run being ranked, or `null` while the active week has not loaded.
   */
  public readonly runNumber = computed<number | null>(
    () => resourceValue(this.bossApi.current, null)?.runNumber ?? null,
  );

  /**
   * Position of the active week inside the run, from one — "semaine 4 sur 10" in the header.
   */
  public readonly runWeekIndex = computed<number | null>(
    () => resourceValue(this.bossApi.current, null)?.runWeekIndex ?? null,
  );

  /**
   * Number of weeks the run spans, or `null` while the active week has not loaded.
   */
  public readonly runWeekCount = computed<number | null>(
    () => resourceValue(this.bossApi.current, null)?.runWeekCount ?? null,
  );

  /**
   * Monday the run opened on, or `null` while the active week has not loaded.
   */
  private readonly runStart = computed<string | null>(() => {
    const currentBoss = resourceValue(this.bossApi.current, null);
    if (!currentBoss) {
      return null;
    }

    return addDays(currentBoss.weekStart, (1 - currentBoss.runWeekIndex) * 7);
  });

  /**
   * The run so far, one entry per player, best total first.
   *
   * Empty while the run's bounds are unknown rather than falling back to every week ever played: a
   * board labelled "campagne" that quietly summed two runs would be wrong in a way nothing on screen
   * could reveal.
   */
  public readonly entries = computed<readonly CampaignRankingEntry[]>(() => {
    const runStart = this.runStart();
    if (runStart === null) {
      return [];
    }

    const totals = new Map<number, Accumulator>();

    for (const week of resourceValue(this.rankingApi.history, null)?.content ?? []) {
      if (week.weekStart < runStart) {
        continue;
      }

      for (const entry of week.ranking) {
        this.accumulate(totals, entry.playerId, entry.displayName, entry);
      }
    }

    // The live week comes from the live ranking, whose entries carry the player object rather than a
    // flat id. It is deliberately included: a run is ranked as it is played, not once it is over.
    for (const entry of resourceValue(this.rankingApi.current, null)?.ranking ?? []) {
      this.accumulate(totals, entry.player.id, entry.player.displayName, entry);
    }

    const ordered = [...totals.values()].sort(
      (left, right) => right.totalDamage - left.totalDamage || left.playerId - right.playerId,
    );

    let position = 1;
    return ordered.map((accumulator) => ({
      position: accumulator.ranked ? position++ : null,
      playerId: accumulator.playerId,
      displayName: accumulator.displayName,
      totalDamage: accumulator.totalDamage,
      bonus: accumulator.bonus,
      completedChallenges: accumulator.completedChallenges,
      activeDays: accumulator.activeDays,
      bossCount: accumulator.bossCount,
    }));
  });

  /**
   * Folds one week of one player into their running total.
   *
   * @param totals - Accumulators keyed by player id, mutated in place.
   * @param playerId - Internal player identifier.
   * @param displayName - Name shown on the board, taken from the most recent week seen.
   * @param week - That player's week, live or finalized.
   */
  private accumulate(
    totals: Map<number, Accumulator>,
    playerId: number,
    displayName: string,
    week: CampaignWeek,
  ): void {
    const accumulator = totals.get(playerId) ?? {
      playerId,
      displayName,
      totalDamage: 0,
      bonus: 0,
      completedChallenges: 0,
      activeDays: 0,
      bossCount: 0,
      ranked: false,
    };

    accumulator.displayName = displayName;
    accumulator.totalDamage += week.totalDamage;
    accumulator.bonus += week.regularityBonus + week.teamBonus;
    accumulator.completedChallenges += week.completedChallenges;
    accumulator.activeDays += week.activeDays;
    accumulator.ranked = accumulator.ranked || week.position !== null;

    if (bossDamageOf(week) > 0) {
      accumulator.bossCount += 1;
    }

    totals.set(playerId, accumulator);
  }

  /**
   * Retries every request the run is folded from.
   *
   * Exposed because {@link hasError} reports their combined state and cannot say which of the three
   * failed — and one of them is the active fight, which nothing outside this service reads and a
   * caller retrying "the rankings" would therefore leave broken.
   */
  public reload(): void {
    reloadAll(this.bossApi.current, this.rankingApi.current, this.rankingApi.history);
  }
}

/**
 * One player's week as the run folds it: the fields both a live and a finalized entry carry.
 *
 * Stated rather than derived from either model, because the two disagree on exactly one field —
 * a finalized entry is always positioned, a live one may not be — and a `Pick` intersecting the two
 * would silently narrow `position` back to a number and lose the inactive case.
 */
interface CampaignWeek {
  readonly position: number | null;
  readonly totalDamage: number;
  readonly regularityBonus: number;
  readonly teamBonus: number;
  readonly completedChallenges: number;
  readonly activeDays: number;
}

/**
 * One player's running total while the run is being folded, before it is frozen into an entry.
 */
interface Accumulator {
  readonly playerId: number;
  displayName: string;
  totalDamage: number;
  bonus: number;
  completedChallenges: number;
  activeDays: number;
  bossCount: number;

  /**
   * Whether the player held a ranking slot on any week of the run. A player deactivated mid-run kept
   * the weeks they played in full — they were in the campaign for them — so the run still ranks them.
   */
  ranked: boolean;
}

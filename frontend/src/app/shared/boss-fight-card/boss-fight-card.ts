import { NgOptimizedImage } from '@angular/common';
import { Component, computed, inject, input } from '@angular/core';
import { LucideFlame, LucideHammer, LucideSkull, LucideSwords } from '@lucide/angular';

import { BossApi } from '@core/boss/boss-api';
import { resolveBossNumberLabel } from '@core/boss/boss-visual.utils';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { ChallengeWeakPoint } from '@core/challenges/challenge-weak-point.model';
import { resolveChallengeWeakPoints } from '@core/challenges/challenge-weak-point.utils';
import { ColonyView } from '@core/colony/colony-view';
import { anyError, anyLoading, resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { ResourceState } from '@shared/resource-state/resource-state';

/**
 * Hero-sized boss fight card: portrait, health bar, weak points and what the fight pays the
 * colony out — the same card `/week` leads with, made a standalone drop-in so `/overview` ("La
 * colonie") can carry it too instead of the smaller linked summary it used before. Self-fetches
 * off the shared `BossApi`/`ChallengesApi`/`ColonyView` resources, so dropping it into a page
 * triggers no request beyond what that page already shares with `/week`.
 */
@Component({
  selector: 'app-boss-fight-card',
  imports: [
    TranslatePipe,
    LucideFlame,
    LucideHammer,
    LucideSkull,
    LucideSwords,
    NgOptimizedImage,
    ResourceState,
  ],
  templateUrl: './boss-fight-card.html',
  styleUrl: './boss-fight-card.css',
})
export class BossFightCard {
  /**
   * Whether to carry the payout aside beside the fight — on for `/week`, where the fight is the
   * whole page and has the width to spare; off for `/overview`'s summary, which sits half-width
   * next to "La journée" and has no room for a second column.
   */
  public readonly showRewards = input(true);

  private readonly bossApi = inject(BossApi);
  private readonly challengesApi = inject(ChallengesApi);
  private readonly colony = inject(ColonyView);
  private readonly translation = inject(Translation);

  protected readonly bossResource = this.bossApi.current;

  protected readonly isLoading = anyLoading(this.bossResource, this.challengesApi.current);
  protected readonly hasError = anyError(this.bossResource, this.challengesApi.current);

  /**
   * The active week's boss confrontation, or `null` until it has loaded.
   */
  protected readonly boss = computed(() => resourceValue(this.bossResource, null));

  /**
   * Resolves a run week's position into its zero-padded `"01"`–`"10"` label, the same way the
   * campaign map and the accueil's week summary already identify a boss.
   */
  protected readonly bossNumberLabel = resolveBossNumberLabel;

  /**
   * Whether the squad has already brought this week's boss down.
   */
  protected readonly defeated = computed(() => {
    const boss = this.boss();
    return !!boss && boss.totalDamageDealt >= boss.effectiveHp;
  });

  /**
   * The colony's own read of this week's fight — rewards banked or still on the table, and what a
   * held boss would cost. `null` on a colony the run has nothing to report on yet.
   */
  protected readonly colonyBoss = computed(
    () => this.colony.bosses().find((week) => week.state === 'CURRENT') ?? null,
  );

  /**
   * What a held boss always costs, regardless of category — read once from the colony rather than
   * recomputed here, since it is not this card's own rule to know.
   */
  protected readonly defeatMoraleLabel = this.colony.defeatMoraleLabel;

  protected readonly bossCategoryKey = computed(() => {
    const category = this.boss()?.boss.category;

    return category ? `boss.category.${category}` : '';
  });

  /**
   * The week's five challenges, reduced to a tier mark and an amount — what actually brings the
   * boss's bar down.
   */
  protected readonly weakPoints = computed<readonly ChallengeWeakPoint[]>(() =>
    resolveChallengeWeakPoints(
      resourceValue(this.challengesApi.current, null)?.challenges ?? [],
      this.translation.language(),
    ),
  );

  /**
   * Share of hit points left, `0`–`100` — the bar always reads this, whatever the outcome so far.
   */
  protected readonly remainingPercentage = computed(() => {
    const boss = this.boss();
    if (!boss || boss.effectiveHp <= 0) {
      return 0;
    }

    return Math.max(
      0,
      Math.min(100, ((boss.effectiveHp - boss.totalDamageDealt) / boss.effectiveHp) * 100),
    );
  });

  /**
   * Hit points left, grouped in the active language (`"37 950"`).
   */
  protected readonly remainingHpLabel = computed(() => {
    const boss = this.boss();

    return boss
      ? formatDamage(
          Math.max(0, boss.effectiveHp - boss.totalDamageDealt),
          this.translation.language(),
        )
      : '';
  });

  /**
   * Total hit points, grouped the same way as {@link remainingHpLabel}.
   */
  protected readonly effectiveHpLabel = computed(() => {
    const boss = this.boss();

    return boss ? formatDamage(boss.effectiveHp, this.translation.language()) : '';
  });

  /**
   * Reloads the backing resources after a failure.
   */
  protected reload(): void {
    this.bossResource.reload();
    this.challengesApi.current.reload();
  }
}

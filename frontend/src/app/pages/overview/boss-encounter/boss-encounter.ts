import { NgOptimizedImage } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { LucideSkull, LucideSwords } from '@lucide/angular';

import { BossApi } from '@core/boss/boss-api';
import { resolveBossHpBarColorClass } from '@core/boss/boss-visual.utils';
import { anyError, anyLoading, resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { ResourceState } from '@shared/resource-state/resource-state';

/**
 * "Weekly boss" card of the overview page.
 *
 * Reframes the week's collective damage — already folded into the ranking's damage by
 * `Leaderboard` — as a single shared health bar: the boss the group is fighting this week, its
 * remaining hit points, and whether it has already fallen. The countdown to the week's end is
 * shown once, in the overview header, rather than repeated here.
 */
@Component({
  selector: 'app-boss-encounter',
  imports: [TranslatePipe, ResourceState, NgOptimizedImage, LucideSkull, LucideSwords],
  templateUrl: './boss-encounter.html',
})
export class BossEncounter {
  /**
   * Data-access service backing the shared current-boss resource.
   */
  private readonly bossApi = inject(BossApi);

  /**
   * Reactive resource fetching the active week's boss confrontation.
   */
  protected readonly bossResource = this.bossApi.current;

  /**
   * Whether the backing resource is still loading.
   */
  protected readonly isLoading = anyLoading(this.bossResource);

  /**
   * Whether the backing resource failed to load.
   */
  protected readonly hasError = anyError(this.bossResource);

  /**
   * Active week's boss confrontation, or `null` while unavailable.
   */
  protected readonly boss = computed(() => resourceValue(this.bossResource, null));

  /**
   * Hit points the boss has left, floored at zero once damage dealt reaches its effective hit
   * points.
   */
  protected readonly remainingHp = computed(() => {
    const boss = this.boss();
    return boss ? Math.max(0, boss.effectiveHp - boss.totalDamageDealt) : 0;
  });

  /**
   * Share of hit points the boss has left, from 0 to 100.
   */
  protected readonly remainingPercentage = computed(() => {
    const boss = this.boss();
    return boss && boss.effectiveHp > 0
      ? Math.max(0, Math.round((this.remainingHp() / boss.effectiveHp) * 100))
      : 0;
  });

  /**
   * Whether the group has already brought the boss down this week.
   */
  protected readonly defeated = computed(() => {
    const boss = this.boss();
    return !!boss && boss.totalDamageDealt >= boss.effectiveHp;
  });

  /**
   * Fill color utility for the health bar, reflecting how close the boss is to falling.
   */
  protected readonly hpBarColorClass = computed(() =>
    resolveBossHpBarColorClass(this.remainingPercentage()),
  );

  /**
   * Reloads the backing resource after a failure.
   */
  protected reload(): void {
    this.bossResource.reload();
  }
}

import { NgOptimizedImage } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { LucideSkull, LucideSwords } from '@lucide/angular';

import { BossApi } from '@core/boss/boss-api';
import { resolveBossHpBarColorClass } from '@core/boss/boss-visual.utils';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { anyError, anyLoading, resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
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
   * i18n service read for the active language when grouping hit-point amounts.
   */
  private readonly translation = inject(Translation);

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
  private readonly remainingHp = computed(() => {
    const boss = this.boss();
    return boss ? Math.max(0, boss.effectiveHp - boss.totalDamageDealt) : 0;
  });

  /**
   * Remaining and total hit points, grouped in the active language (`"66 500"`).
   *
   * Grouped rather than printed raw: the campaign timeline already formats the very same figures
   * that way (see `Boss`), and a five-digit health pool is unreadable without the separator.
   */
  protected readonly remainingHpLabel = computed(() =>
    formatDamage(this.remainingHp(), this.translation.language()),
  );

  /**
   * Total hit points, grouped like {@link remainingHpLabel}.
   */
  protected readonly effectiveHpLabel = computed(() =>
    formatDamage(this.boss()?.effectiveHp ?? 0, this.translation.language()),
  );

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

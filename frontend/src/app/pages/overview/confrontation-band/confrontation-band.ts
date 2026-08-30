import { Component, computed, inject, input } from '@angular/core';
import { LucideSkull, LucideUsers } from '@lucide/angular';

import { BossApi } from '@core/boss/boss-api';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { ColonyView } from '@core/colony/colony-view';
import { RemainingTime } from '@core/date/week-period.utils';
import { resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';

/**
 * The week, read as the confrontation it is: the squad on the left, the clock cut into the middle,
 * the threat on the right.
 *
 * This replaces the two stacked cards `/overview` used to open with — a turnout panel and, inside a
 * second panel, the fight's own hero card. Two frames, two backgrounds and two cut corners for what
 * is one question: how are we doing against this week's boss, and how long is left. Read left to
 * right the band answers it in one line — this is us, this is the time we have, this is what we are
 * shooting at.
 *
 * The week's challenges appear here only as the ally side's round pips. Their breakdown as the
 * boss's weak points stays on `/week` and `/campaign`, where there is room to say what each one
 * actually asks for.
 *
 * Self-fetches off the shared resources, exactly like `BossFightCard`, so it costs no request the
 * page does not already make.
 */
@Component({
  selector: 'app-confrontation-band',
  imports: [TranslatePipe, LucideSkull, LucideUsers],
  templateUrl: './confrontation-band.html',
  styleUrl: './confrontation-band.css',
})
export class ConfrontationBand {
  /**
   * Time left in the week, or `null` while loading. Passed in rather than derived here so the page
   * keeps one ticker for a figure two blocks read.
   */
  public readonly remaining = input<RemainingTime | null>(null);

  private readonly bossApi = inject(BossApi);
  private readonly challengesApi = inject(ChallengesApi);
  private readonly translation = inject(Translation);

  protected readonly colony = inject(ColonyView);

  /**
   * The active week's fight, or `null` until it has loaded.
   */
  protected readonly boss = computed(() => resourceValue(this.bossApi.current, null));

  /**
   * The week's challenges, in the order they were drawn.
   */
  private readonly challenges = computed(
    () => resourceValue(this.challengesApi.current, null)?.challenges ?? [],
  );

  /**
   * Which weak points have already landed, by position — a challenge counts as cleared only once the
   * whole squad has validated it, the same rule `TeamProgress` applies.
   */
  protected readonly clearedFlags = computed<readonly boolean[]>(() =>
    this.challenges().map(
      (challenge) =>
        challenge.totalPlayers > 0 && challenge.completedPlayers === challenge.totalPlayers,
    ),
  );

  protected readonly clearedCount = computed(
    () => this.clearedFlags().filter((cleared) => cleared).length,
  );

  protected readonly challengeCount = computed(() => this.challenges().length);

  /**
   * Share of hit points left, `0`–`100`, which is what the bar draws.
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
   * Hit points left and the boss's full health, both grouped in the active language.
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

  protected readonly effectiveHpLabel = computed(() => {
    const boss = this.boss();

    return boss ? formatDamage(boss.effectiveHp, this.translation.language()) : '';
  });

  /**
   * Translation key of the boss's weight class, read beside the "menace" tag as the qualifier it is.
   */
  protected readonly categoryKey = computed(() => {
    const category = this.boss()?.boss.category;

    return category ? `boss.category.${category}` : '';
  });

  /**
   * Whether the squad has already brought the boss down, which is what the bar reads as empty.
   */
  protected readonly defeated = computed(() => {
    const boss = this.boss();

    return !!boss && boss.totalDamageDealt >= boss.effectiveHp;
  });
}

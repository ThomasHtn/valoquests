import { Component, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideFlame, LucideHammer, LucideSkull } from '@lucide/angular';

import { BossApi } from '@core/boss/boss-api';
import { BossFall } from '@core/boss/boss-fall';
import { resolveBossNumberLabel } from '@core/boss/boss-visual.utils';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { resolveChallengeWeakPoints } from '@core/challenges/challenge-weak-point.utils';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { ColonyView } from '@core/colony/colony-view';
import { RULE_ANCHOR } from '@core/rules/rule-anchor.constants';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { ResourceState } from '@shared/resource-state/resource-state';
import { TierGlyph } from '@shared/tier-glyph/tier-glyph';

/**
 * The week's fight, and what winning it is worth.
 *
 * Two blocks on one band: the boss on the left, what Sunday pays on the right. Neither is a card —
 * the band is flat, its threat edge is an inset rule, and the two halves are told apart by a
 * hairline, the same furniture `/overview` was rebuilt on.
 *
 * Two things the fight card it replaces never said. The boss has a **name**: `boss.name` and its
 * flavour line were both in the response and written nowhere on this page, which identified the
 * fight by its week number alone. And the payout is read as **the step of the ladder it buys**
 * (see `ColonyView.bossPayout`) rather than as a bare `+480 matériaux`, an amount with no scale
 * next to it. The row of weak points it always carried is now marks alone, no amounts: the five
 * cards immediately below hold every figure, and repeating them here made the band restate a
 * section of its own page.
 *
 * Self-fetches off the shared `BossApi`, `ChallengesApi` and `ColonyView` resources, so it costs no
 * request beyond what the page already asks for.
 */
/**
 * How long the fall sequence runs, in milliseconds — the strike, then the two rewards rising behind
 * it. Matches the keyframes in `boss-band.css`; the two are one thing declared twice, so they move
 * together or not at all.
 */
const CELEBRATION_MS = 1600;

@Component({
  selector: 'app-boss-band',
  imports: [
    TranslatePipe,
    RouterLink,
    LucideFlame,
    LucideHammer,
    LucideSkull,
    ResourceState,
    TierGlyph,
  ],
  templateUrl: './boss-band.html',
  styleUrl: './boss-band.css',
})
export class BossBand {
  private readonly bossApi = inject(BossApi);
  private readonly challengesApi = inject(ChallengesApi);
  private readonly colony = inject(ColonyView);
  private readonly translation = inject(Translation);

  /**
   * Remembers which weeks' falls have already been watched.
   */
  private readonly bossFall = inject(BossFall);

  /**
   * Named rulebook fragments, so the link beside the hit-point figure lands on the beat that
   * explains where that figure comes from.
   */
  protected readonly ruleAnchor = RULE_ANCHOR;

  protected readonly bossResource = this.bossApi.current;

  protected readonly isLoading = anyLoading(this.bossResource, this.challengesApi.current);
  protected readonly hasError = anyError(this.bossResource, this.challengesApi.current);

  /**
   * The active week's boss confrontation, or `null` until it has loaded.
   */
  // `?? null` collapses the resource's own `undefined` into the same empty the fallback uses, so the
  // template's `@if … as` narrows to the confrontation itself — the arrangement `week.ts` uses too.
  protected readonly boss = computed(() => resourceValue(this.bossResource, null) ?? null);

  /**
   * What the fight pays the colony, and what a boss left standing costs it.
   */
  protected readonly payout = this.colony.bossPayout;

  /**
   * The week's five challenges, reduced to their tier marks — what actually brings the bar down.
   *
   * Marks only, no amounts: the five cards right below carry every figure, and repeating them here
   * made the band restate a section of its own page. What the row is for is the shape of the fight
   * — five weak points, from the easiest to the one that hurts most.
   */
  protected readonly weakPoints = computed(() =>
    resolveChallengeWeakPoints(
      resourceValue(this.challengesApi.current, null)?.challenges ?? [],
      this.translation.language(),
    ),
  );

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

    return boss !== null && boss.totalDamageDealt >= boss.effectiveHp;
  });

  /**
   * Whether the fall is being played right now.
   *
   * Drives the one orchestrated moment in the application: the card takes the blow, then the two
   * rewards it banked rise one after the other. Off again a beat later, leaving the settled state —
   * the sequence is an event, not a mode.
   */
  protected readonly celebrating = signal(false);

  /**
   * The boss's weight class, as the key naming it.
   */
  protected readonly categoryKey = computed(() => {
    const category = this.boss()?.boss.category;

    return category ? `boss.category.${category}` : '';
  });

  /**
   * Share of hit points left, `0`–`100` — the bar always reads this, whatever the outcome so far.
   */
  protected readonly remainingPercentage = computed(() => {
    const boss = this.boss();
    if (boss === null || boss.effectiveHp <= 0) {
      return 0;
    }

    return Math.max(
      0,
      Math.min(100, ((boss.effectiveHp - boss.totalDamageDealt) / boss.effectiveHp) * 100),
    );
  });

  /**
   * Whether the bar has an edge to draw: a fight already started and not yet finished.
   */
  protected readonly isDraining = computed(() => {
    const remaining = this.remainingPercentage();

    return remaining > 0 && remaining < 100;
  });

  /**
   * Hit points left, grouped in the active language (`"37 950"`).
   */
  protected readonly remainingHpLabel = computed(() => {
    const boss = this.boss();

    return boss === null
      ? ''
      : formatDamage(
          Math.max(0, boss.effectiveHp - boss.totalDamageDealt),
          this.translation.language(),
        );
  });

  /**
   * Total hit points, grouped the same way as {@link remainingHpLabel}.
   */
  protected readonly effectiveHpLabel = computed(() => {
    const boss = this.boss();

    return boss === null ? '' : formatDamage(boss.effectiveHp, this.translation.language());
  });

  /**
   * Damage the squad has already taken off the bar this week, grouped the same way.
   */
  protected readonly damageDealtLabel = computed(() => {
    const boss = this.boss();

    return boss === null ? '' : formatDamage(boss.totalDamageDealt, this.translation.language());
  });

  /**
   * Plays the fall once per week, on the first visit that finds the boss down.
   *
   * `prefers-reduced-motion` skips straight to marking it seen: the settled state carries the same
   * information, and somebody who asked for no motion should not have to sit through a sequence to
   * reach it.
   */
  constructor() {
    effect(() => {
      const boss = this.boss();
      if (boss === null || !this.defeated() || !this.bossFall.isUnseen(boss.weekStart)) {
        return;
      }

      this.bossFall.markSeen(boss.weekStart);

      if (matchMedia('(prefers-reduced-motion: reduce)').matches) {
        return;
      }

      this.celebrating.set(true);
      setTimeout(() => this.celebrating.set(false), CELEBRATION_MS);
    });
  }

  /**
   * Reloads every backing resource after a failure.
   */
  protected reload(): void {
    reloadAll(this.bossResource, this.challengesApi.current);
  }
}

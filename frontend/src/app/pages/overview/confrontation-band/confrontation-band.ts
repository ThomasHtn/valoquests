import { Component, computed, inject, input } from '@angular/core';
import { LucideSkull, LucideUsers } from '@lucide/angular';

import { BossApi } from '@core/boss/boss-api';
import { bossDamageOf } from '@core/boss/boss-damage.utils';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { ColonyView } from '@core/colony/colony-view';
import { ColonyPresenceState } from '@core/colony/colony.model';
import { RemainingTime } from '@core/date/week-period.utils';
import { resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { RankingApi } from '@core/ranking/ranking-api';
import { Tooltip } from '@shared/tooltip/tooltip';

/**
 * Order the roster's presence tokens are grouped in, so the row reads as a gauge rather than as a
 * pattern with holes: counted first, then those who played short of the threshold, then the rest.
 */
const PRESENCE_ORDER: Record<ColonyPresenceState, number> = { FULL: 0, PARTIAL: 1, NONE: 2 };

/**
 * One presence token of the squad's roster row: whether it is lit, and who it stands for.
 */
interface RosterSlot {
  /**
   * Stable identity for the `@for` track expression — the player's own id.
   */
  readonly key: string;

  /**
   * Whether the player cleared today's threshold, which is what lights the hexagon.
   */
  readonly present: boolean;

  /**
   * Already-translated bubble naming the player and saying how their day went.
   */
  readonly tooltip: string;
}

/**
 * One stretch of the squad's assault bar: what a single player tore off the boss this week.
 *
 * `percentage` is a share of the boss's *effective hit points*, never of the squad's own total —
 * that is what puts the two camps on one scale, so the blue the squad has taken is exactly as wide
 * on its own bar as it is on the boss's.
 */
interface AssaultSegment {
  /**
   * Stable identity for the `@for` track expression.
   */
  readonly key: string;

  /**
   * Already-translated bubble naming the segment and pricing it.
   */
  readonly tooltip: string;

  /**
   * Width of the stretch, `0`–`100`, as a share of the boss's effective hit points.
   */
  readonly percentage: number;
}

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
 * Laid out as a fight card: both camps carry the same five rows, so they are compared straight
 * across rather than read one after the other, and their two gauges are the same measurement drawn
 * from both ends. The squad's is its damage broken down per player; the boss's is its health,
 * emptied from the seam outwards. One scale — the boss's effective hit points — governs both, so the
 * blue filled in on the left is exactly as long as the track cleared on the right.
 *
 * The week's challenges have no gauge here any more. Their damage is inside the squad's bar already
 * — a player's stretch is everything they scored — and which challenge asks for what stays on
 * `/week` and `/campaign`, where there is room to say it.
 *
 * Self-fetches off the shared resources, exactly like `BossFightCard`, so it costs no request the
 * page does not already make — the ranking behind the damage breakdown is the same one
 * `MiniRanking` renders three rows of further down the page.
 */
@Component({
  selector: 'app-confrontation-band',
  imports: [TranslatePipe, LucideSkull, LucideUsers, Tooltip],
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
  private readonly rankingApi = inject(RankingApi);
  private readonly translation = inject(Translation);

  protected readonly colony = inject(ColonyView);

  /**
   * The active week's fight, or `null` until it has loaded.
   */
  protected readonly boss = computed(() => resourceValue(this.bossApi.current, null));

  /**
   * The boss's effective hit points, or `0` while the fight has not loaded — the scale both gauges
   * are drawn against.
   */
  private readonly effectiveHp = computed(() => this.boss()?.effectiveHp ?? 0);

  /**
   * What each player has torn off the boss this week, biggest share first.
   *
   * Read off the live ranking rather than off a dedicated endpoint, the same reconstruction
   * `BossCampaign` makes for the campaign's per-week board: a player's weekly total minus the
   * regularity bonus *is* what the boss took from them (see {@link bossDamageOf}).
   *
   * Inactive players are left out: they hold no ranking position, and they deal the boss no damage
   * at all. So is anyone still on zero — a segment of no width is not a segment, and it would put
   * an unreachable tooltip in the bar.
   */
  protected readonly damageSegments = computed<readonly AssaultSegment[]>(() => {
    const effectiveHp = this.effectiveHp();
    if (effectiveHp <= 0) {
      return [];
    }

    const language = this.translation.language();

    return (resourceValue(this.rankingApi.current, null)?.ranking ?? [])
      .filter((entry) => typeof entry.position === 'number')
      .map((entry) => ({ entry, damage: bossDamageOf(entry) }))
      .filter(({ damage }) => damage > 0)
      .sort((left, right) => right.damage - left.damage)
      .map(({ entry, damage }) => ({
        key: `${entry.player.id}`,
        tooltip: this.translation.translate('overview.confrontation.segment', {
          player: entry.player.displayName,
          damage: formatDamage(damage, language),
        }),
        percentage: (damage / effectiveHp) * 100,
      }));
  });

  /**
   * The whole squad bar said in one sentence, for readers who get no hover.
   *
   * The bar is `role="img"`: a screen reader announces this instead of walking a row of unlabelled
   * stretches, and the per-segment bubbles stay what they are — an enhancement for a pointer.
   */
  protected readonly damageBreakdownLabel = computed(() =>
    this.translation.translate('overview.confrontation.breakdown', {
      breakdown: this.damageSegments()
        .map((segment) => segment.tooltip)
        .join(' · '),
    }),
  );

  /**
   * One token per roster member, lit for whoever cleared today's threshold, each naming its player.
   *
   * Read off `ColonyView.presencePips`, which already resolves the colony's own presence roll into a
   * player and a translated sentence per entry — the row used to be built from the head count alone,
   * which drew the right number of tokens but had nothing to say about any of them.
   *
   * Sorted rather than left in roster order, and that is the whole reason the row reads: grouped, it
   * is a gauge with a lit run and an empty tail; scattered, it is a pattern with holes the reader has
   * to count. Within the unlit tail the near-misses come first, so the squad's own order runs from
   * "counted" through "played, short of the bar" to "has not played".
   *
   * Only `FULL` lights a token, which is exactly what the tag above counts. A `PARTIAL` player sits
   * unlit and their bubble says why — the nuance the two-state row cannot draw.
   */
  protected readonly rosterSlots = computed<readonly RosterSlot[]>(() =>
    [...this.colony.presencePips()]
      .sort((left, right) => PRESENCE_ORDER[left.state] - PRESENCE_ORDER[right.state])
      .map((pip) => ({
        key: `${pip.playerId}`,
        present: pip.state === 'FULL',
        tooltip: pip.ariaLabel,
      })),
  );

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
   * Share of hit points already torn off — the bare stretch of track, and the point the fill has
   * been pushed back to.
   *
   * Taken from the boss's own total rather than by summing the squad's segments: the health bar and
   * the figure above it are the fight's record, and the breakdown is a reconstruction of it.
   */
  protected readonly lostPercentage = computed(() => 100 - this.remainingPercentage());

  /**
   * Whether the bar has an edge to draw: a fight already started and not yet finished.
   */
  protected readonly isDraining = computed(() => {
    const remaining = this.remainingPercentage();

    return remaining > 0 && remaining < 100;
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
   * What the squad has already torn off, which is the figure its own bar is a breakdown of.
   *
   * Stated on the squad's side rather than under the boss's health: the bar below it is the squad's
   * output, and a figure belongs above the gauge that draws it.
   */
  protected readonly damageDealtLabel = computed(() => {
    const boss = this.boss();

    return boss ? formatDamage(boss.totalDamageDealt, this.translation.language()) : '';
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

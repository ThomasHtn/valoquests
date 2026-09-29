import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { LucideCheck, LucideUsers, LucideZap } from '@lucide/angular';

import { resolveLocale } from '@core/i18n/locale.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Avatar } from '@shared/avatar/avatar';
import { Tooltip } from '@shared/tooltip/tooltip';
import { MAX_NOTCHED_TARGET, MAX_NOTCHED_TARGET_PHONE } from '../challenges.constants';
import { ChallengeCard } from '../challenges.model';
import { describeRung } from '../challenges.utils';

/**
 * One challenge: the hexagon beside the key line and the name, what it brings back per operator,
 * what it asks, then the rule and one band per operator closing toward the target, and the count
 * of operators who finished it.
 *
 * Shared by the day's challenge and the week's five, so both read as the same object. A caller
 * can project a `[cardFoot]` element at the start of the last line, the day's countdown for one.
 */
@Component({
  selector: 'app-challenge-card',
  imports: [TranslatePipe, Tooltip, Avatar, LucideCheck, LucideUsers, LucideZap],
  templateUrl: './challenge-card.html',
  styleUrl: './challenge-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[class.card--done]': 'allDone()',
    '[class.card--wide]': 'wide()',
    '[class.card--dense]': 'dense()',
    '[style.--tone]': 'card().tone',
  },
})
export class ChallengeCardView {
  public readonly card = input.required<ChallengeCard>();

  /**
   * Whether the card spans the whole column: its bands then run on two columns.
   */
  public readonly wide = input(false);

  /**
   * i18n service wording the bands' tooltips.
   */
  private readonly translation = inject(Translation);

  /**
   * Whether every operator validated it: the card then turns green.
   */
  protected readonly allDone = computed(() => {
    const { rungs, doneCount } = this.card();
    return rungs.length > 0 && doneCount === rungs.length;
  });

  /**
   * Whether the target counts too many units for a phone's narrower band: its notches are then
   * dropped there.
   */
  protected readonly dense = computed(() => (this.card().target ?? 0) > MAX_NOTCHED_TARGET_PHONE);

  /**
   * Positions of the notches along a band, in percent: one per unit when the target counts few
   * of them, none otherwise.
   */
  protected readonly notches = computed<readonly number[]>(() => {
    const target = this.card().target;
    if (target === null || target < 2 || target > MAX_NOTCHED_TARGET) {
      return [];
    }
    return Array.from({ length: target - 1 }, (_, index) => ((index + 1) / target) * 100);
  });

  /**
   * One tooltip per band, in the rungs' order: the exact figures behind the compact labels and the
   * share of the target reached.
   */
  protected readonly bandTips = computed<readonly string[]>(() => {
    const { rungs, target } = this.card();
    const locale = resolveLocale(this.translation.language());
    return rungs.map((rung) =>
      describeRung(rung, target, locale, (key, params) =>
        this.translation.translate(`challenges.card.bandTooltip.${key}`, params),
      ),
    );
  });
}

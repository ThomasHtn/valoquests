import { Component, computed, input } from '@angular/core';
import { LucideSwords } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';

/**
 * Damage reward rendered as a sword icon next to the amount.
 *
 * Shared by the weekly challenges card and both ranking tables so a reward reads the same
 * everywhere it appears, whether it is the amount a challenge is worth or the total a player has
 * dealt. The host element is the badge itself: an outline pill (border only, no fill) rather than
 * a solid one, so it stays legible without competing with the surrounding solid-fill badges
 * (position, completed challenges).
 */
@Component({
  selector: 'app-damage-badge',
  imports: [TranslatePipe, LucideSwords],
  template: `
    <svg aria-hidden="true" class="h-3 w-3" lucideSwords></svg>
    {{ damage() }}
    <span class="sr-only">{{ 'shared.damageBadge.label' | translate }}</span>
  `,
  host: {
    class:
      'inline-flex items-center gap-1 rounded-full border px-2 py-1 text-xs font-bold tabular-nums',
    '[class]': 'toneClass()',
  },
})
export class DamageBadge {
  /**
   * Amount of damage to display.
   */
  public readonly damage = input.required<number>();

  /**
   * Colors matching the current amount.
   *
   * A zero score is rendered neutral rather than red: the red treatment reads as a reward, and a
   * player who has dealt nothing yet was being congratulated for it.
   */
  protected readonly toneClass = computed(() =>
    this.damage() === 0
      ? 'border-surface-700 text-text-muted'
      : 'border-accent-red/40 text-accent-red',
  );
}

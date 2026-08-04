import { Component, computed, input } from '@angular/core';

import { TranslatePipe } from '@core/i18n/translate-pipe';

/**
 * Point reward rendered as a badge.
 *
 * Shared by the weekly challenges card and both ranking tables so a reward reads the same
 * everywhere it appears, whether it is the amount a challenge is worth or the total a player has
 * earned. The host element is the badge itself.
 */
@Component({
  selector: 'app-points-badge',
  imports: [TranslatePipe],
  template: '{{ points() }} {{ "shared.pointsBadge.unit" | translate }}',
  host: {
    class: 'inline-flex items-center rounded-full px-2.5 py-1 text-xs font-bold tabular-nums',
    '[class]': 'toneClass()',
  },
})
export class PointsBadge {
  /**
   * Amount of points to display.
   */
  public readonly points = input.required<number>();

  /**
   * Colors matching the current amount.
   *
   * A zero score is rendered neutral rather than gold: the gold treatment reads as a reward, and a
   * player who has earned nothing yet was being congratulated for it.
   */
  protected readonly toneClass = computed(() =>
    this.points() === 0 ? 'bg-surface-700 text-text-muted' : 'bg-accent-gold/15 text-accent-gold',
  );
}

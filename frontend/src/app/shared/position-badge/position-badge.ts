import { Component, computed, input } from '@angular/core';

import { resolvePositionBadgeClass } from '@core/ranking/ranking-visual.utils';

/**
 * Ranking position ("#1") colored by podium tier.
 */
@Component({
  selector: 'app-position-badge',
  templateUrl: './position-badge.html',
  host: { class: 'contents' },
})
export class PositionBadge {
  /**
   * 1-based position, `null` for an inactive player (renders nothing).
   */
  public readonly position = input.required<number | null>();

  /**
   * Tailwind text color of the position.
   */
  protected readonly colorClass = computed(() => resolvePositionBadgeClass(this.position()));
}

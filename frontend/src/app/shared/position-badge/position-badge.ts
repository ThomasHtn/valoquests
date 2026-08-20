import { Component, computed, input } from '@angular/core';

import { resolvePositionBadgeClass } from '@core/ranking/ranking-visual.utils';

/**
 * Text badge displaying a ranking position (e.g. "#1"), colored by podium tier.
 *
 * Shared by the podium, the weekly ranking and the ranking history page so a position reads the
 * same everywhere it appears.
 */
@Component({
  selector: 'app-position-badge',
  templateUrl: './position-badge.html',
  host: { class: 'contents' },
})
export class PositionBadge {
  /**
   * 1-based ranking position to display, or {@code null} for an inactive player who never
   * consumes a ranking slot.
   */
  public readonly position = input.required<number | null>();

  /**
   * Tailwind text color utility applied to the position number.
   */
  protected readonly colorClass = computed(() => resolvePositionBadgeClass(this.position()));
}

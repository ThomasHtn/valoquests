import { Component, input } from '@angular/core';

/**
 * Ranking position ("#1"), gold for the leader.
 */
@Component({
  selector: 'app-position-badge',
  templateUrl: './position-badge.html',
  styleUrl: './position-badge.scss',
  host: { class: 'contents' },
})
export class PositionBadge {
  /**
   * 1-based position, `null` for an inactive player (renders nothing).
   */
  public readonly position = input.required<number | null>();
}

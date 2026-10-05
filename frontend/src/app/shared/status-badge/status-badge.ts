import { Component, input } from '@angular/core';

import { StatusBadgeTone } from './status-badge.model';

/**
 * A state in words plus a tint, never a tint alone (roster status, running sync).
 */
@Component({
  selector: 'app-status-badge',
  templateUrl: './status-badge.html',
  styleUrl: './status-badge.scss',
  host: {
    class: 'notch-tr notch-tr-edge',
    '[class]': '"status-badge--" + tone()',
  },
})
export class StatusBadge {
  /**
   * Translated state name.
   */
  public readonly label = input.required<string>();

  /**
   * Tint of the badge.
   */
  public readonly tone = input<StatusBadgeTone>('neutral');
}

import { Component, computed, input } from '@angular/core';
import { StatusBadgeTone } from './status-badge.model';
import { TONE_CLASS } from './status-badge.constants';

/**
 * A state, stated in words and tinted — never tinted alone: a roster player's status on their row,
 * whether a synchronization run is still going.
 *
 * Carries the direction's small notched silhouette, `notch-tr-edge` included. One of the two call
 * sites this replaces had the cut without the edge that continues its border along the diagonal,
 * so its corner was left bare where the other's was closed.
 */
@Component({
  selector: 'app-status-badge',
  templateUrl: './status-badge.html',
  host: {
    class:
      'tracking-label notch-tr notch-tr-edge inline-block shrink-0 border px-2.5 py-1 font-mono text-xs font-semibold uppercase [--notch:0.375rem]',
    '[class]': 'toneClass()',
  },
})
export class StatusBadge {
  /**
   * Already-translated name of the state.
   */
  public readonly label = input.required<string>();

  /**
   * Which treatment this badge renders.
   */
  public readonly tone = input<StatusBadgeTone>('neutral');

  /**
   * Resolved Tailwind classes for the current tone.
   */
  protected readonly toneClass = computed(() => TONE_CLASS[this.tone()]);
}

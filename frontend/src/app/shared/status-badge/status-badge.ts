import { Component, computed, input } from '@angular/core';
import { StatusBadgeTone } from './status-badge.model';
import { TONE_CLASS } from './status-badge.constants';

/**
 * A state in words plus a tint, never a tint alone (roster status, running sync).
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
   * Translated state name.
   */
  public readonly label = input.required<string>();

  /**
   * Tint of the badge.
   */
  public readonly tone = input<StatusBadgeTone>('neutral');

  /**
   * Tailwind classes of the current tone.
   */
  protected readonly toneClass = computed(() => TONE_CLASS[this.tone()]);
}

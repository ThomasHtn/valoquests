import { Component, computed, input } from '@angular/core';

/**
 * Weight a status badge carries: the brand tint for a state that is live or current, a neutral one
 * for a state that is merely inert, and the danger tint for one that is out of play.
 */
export type StatusBadgeTone = 'brand' | 'neutral' | 'danger';

const TONE_CLASS: Record<StatusBadgeTone, string> = {
  brand: 'border-brand-500/50 bg-brand-500/12 text-brand-400',
  neutral: 'border-surface-600 bg-surface-800 text-text-secondary',
  danger: 'border-danger/40 bg-danger/10 text-danger',
};

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
  template: `{{ label() }}`,
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

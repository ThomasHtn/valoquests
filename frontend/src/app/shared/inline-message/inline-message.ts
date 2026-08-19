import { Component, computed, input } from '@angular/core';

/**
 * What an inline message reports, each mapped to one rule color below.
 */
export type InlineMessageTone = 'info' | 'success' | 'danger';

const TONE_CLASS: Record<InlineMessageTone, string> = {
  info: 'border-brand-500/60 text-text-secondary',
  success: 'border-success/60 text-success',
  danger: 'border-danger/60 text-danger',
};

/**
 * A line of feedback attached to the block it concerns: the outcome of a backoffice command, the
 * reason a control is locked, the error a run reported.
 *
 * Marked by a colored rule down its leading edge rather than boxed in a tinted panel — a panel
 * that size reads as a section of its own, when this is a remark about the section above it.
 *
 * The three call sites this replaces had each picked their own text size for the same kind of
 * remark; the one kept is `text-prose`, the step the type scale reserves for runs of prose meant
 * to be read rather than scanned.
 */
@Component({
  selector: 'app-inline-message',
  template: `<ng-content />`,
  host: {
    class: 'block border-l-2 pl-3 text-prose text-pretty',
    '[class]': 'toneClass()',
    role: 'status',
  },
})
export class InlineMessage {
  /**
   * Which treatment this message renders.
   */
  public readonly tone = input<InlineMessageTone>('info');

  /**
   * Resolved Tailwind classes for the current tone.
   */
  protected readonly toneClass = computed(() => TONE_CLASS[this.tone()]);
}

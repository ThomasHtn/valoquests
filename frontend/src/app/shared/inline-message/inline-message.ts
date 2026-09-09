import { Component, computed, input } from '@angular/core';
import { InlineMessageTone } from './inline-message.model';
import { TONE_CLASS } from './inline-message.constants';

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
  templateUrl: './inline-message.html',
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

import { Component, computed, input } from '@angular/core';
import { InlineMessageTone } from './inline-message.model';
import { TONE_CLASS } from './inline-message.constants';

/**
 * Feedback line under the block it concerns, marked by a leading rule rather than a panel.
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
   * Tone of the message.
   */
  public readonly tone = input<InlineMessageTone>('info');

  /**
   * Tailwind classes of the current tone.
   */
  protected readonly toneClass = computed(() => TONE_CLASS[this.tone()]);
}

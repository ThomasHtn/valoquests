import { Component, input } from '@angular/core';

import { InlineMessageTone } from './inline-message.model';

/**
 * Feedback line under the block it concerns, marked by a leading rule rather than a panel.
 */
@Component({
  selector: 'app-inline-message',
  templateUrl: './inline-message.html',
  styleUrl: './inline-message.scss',
  host: {
    '[class]': '"inline-message--" + tone()',
    role: 'status',
  },
})
export class InlineMessage {
  /**
   * Tone of the message.
   */
  public readonly tone = input<InlineMessageTone>('info');
}

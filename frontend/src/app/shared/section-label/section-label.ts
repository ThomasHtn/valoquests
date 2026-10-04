import { Component, input } from '@angular/core';

/**
 * Right-aligned micro-label captioning the block below (a total, a filter scope).
 * Negative bottom margin pulls it against that block instead of floating between two.
 */
@Component({
  selector: 'app-section-label',
  templateUrl: './section-label.html',
  host: {
    class: 'flex justify-end',
    '[class.hidden]': '!label()',
    '[class.-mb-3]': '!!label()',
  },
})
export class SectionLabel {
  /**
   * Translated caption; empty renders nothing.
   */
  public readonly label = input('');
}

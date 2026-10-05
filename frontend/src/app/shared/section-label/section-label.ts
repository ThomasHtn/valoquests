import { Component, input } from '@angular/core';

/**
 * Right-aligned micro-label captioning the block below (a total, a filter scope).
 */
@Component({
  selector: 'app-section-label',
  templateUrl: './section-label.html',
  styleUrl: './section-label.scss',
  host: { '[class.section-label--empty]': '!label()' },
})
export class SectionLabel {
  /**
   * Translated caption; empty renders nothing.
   */
  public readonly label = input('');
}

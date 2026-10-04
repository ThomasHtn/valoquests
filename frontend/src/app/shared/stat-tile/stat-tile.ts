import { Component, input } from '@angular/core';

import { Tooltip } from '@shared/tooltip/tooltip';

/**
 * One figure of a statistics strip: micro-label, display value and a top rule.
 */
@Component({
  selector: 'app-stat-tile',
  imports: [Tooltip],
  templateUrl: './stat-tile.html',
  host: { class: 'block border-t-2 border-brand-500 bg-text-primary/4 px-4 py-3.5' },
})
export class StatTile {
  /**
   * Translated name of the figure.
   */
  public readonly label = input.required<string>();

  /**
   * Formatted figure.
   */
  public readonly value = input.required<string | number>();

  /**
   * Text color of the value, for judged figures such as a K/D.
   */
  public readonly valueClass = input('text-text-primary');

  /**
   * Translated explanation shown on hover and focus, empty for none.
   */
  public readonly tooltip = input('');
}

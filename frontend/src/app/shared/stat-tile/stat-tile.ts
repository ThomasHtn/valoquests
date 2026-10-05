import { Component, input } from '@angular/core';

import { Tooltip } from '@shared/tooltip/tooltip';

/**
 * One figure of a statistics strip: micro-label, display value and a top rule.
 */
@Component({
  selector: 'app-stat-tile',
  imports: [Tooltip],
  templateUrl: './stat-tile.html',
  styleUrl: './stat-tile.scss',
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
   * CSS colour of the value, for judged figures such as a K/D.
   */
  public readonly tone = input('var(--color-text-primary)');

  /**
   * Translated explanation shown on hover and focus, empty for none.
   */
  public readonly tooltip = input('');
}

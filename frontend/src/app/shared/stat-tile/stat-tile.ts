import { Component, input } from '@angular/core';

import { Tooltip } from '@shared/tooltip/tooltip';

/**
 * One figure of a statistics strip: its name in a micro-label, the value under it in the display
 * face, and a rule capping the tile instead of a box around it — so a row of them reads as one
 * instrument panel rather than as a set of loose cards.
 *
 * Only the plain shape lives here, which is what every tile but one is. The lead tile of a strip
 * (the win rate on a player's profile) carries a marker, a secondary figure and a bar of its own;
 * it stays written out at its call site rather than pushing three projection slots and as many
 * conditional inputs into this component for a single user.
 */
@Component({
  selector: 'app-stat-tile',
  imports: [Tooltip],
  templateUrl: './stat-tile.html',
  host: { class: 'block border-t-2 border-brand-500/40 bg-text-primary/4 px-4 py-3.5' },
})
export class StatTile {
  /**
   * Already-translated name of the figure.
   */
  public readonly label = input.required<string>();

  /**
   * The figure itself, already formatted.
   */
  public readonly value = input.required<string | number>();

  /**
   * Text color of the value, for the figures the application judges (a K/D reads green or red
   * against its thresholds). Left at the neutral primary for the ones it only reports.
   */
  public readonly valueClass = input('text-text-primary');

  /**
   * Already-translated explanation of what the figure measures and how it is worked out, shown on
   * hover and on keyboard focus. Left empty for a figure whose label already says it all.
   */
  public readonly tooltip = input('');
}

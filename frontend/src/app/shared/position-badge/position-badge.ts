import { Component, computed, input } from '@angular/core';

import { resolvePositionBadgeClass } from '@core/ranking/ranking-visual.utils';
import { POSITION_BADGE_SIZES } from './position-badge.constants';
import { PositionBadgeSize } from './position-badge.model';

/**
 * Hexagonal badge displaying a ranking position, highlighted by podium tier.
 *
 * Shared by the podium, the weekly ranking and the ranking history page so a position reads the
 * same everywhere it appears. Rendered as an SVG polygon rather than a CSS `clip-path` applied to
 * a bordered box: a `clip-path` clips a rectangular border after it is drawn, so the border only
 * lines up with the hexagon on its vertical edges and turns into a blurred blob at the slanted
 * ones. An SVG `stroke` instead follows the polygon's actual path, so every edge stays crisp.
 */
@Component({
  selector: 'app-position-badge',
  templateUrl: './position-badge.html',
  host: { class: 'contents' },
})
export class PositionBadge {
  /**
   * 1-based ranking position to display.
   */
  public readonly position = input.required<number>();

  /**
   * Size preset controlling the badge's dimensions and font size.
   */
  public readonly size = input<PositionBadgeSize>('md');

  /**
   * Rendering metrics matching the current {@link size}.
   */
  protected readonly metrics = computed(() => POSITION_BADGE_SIZES[this.size()]);

  /**
   * Whether the position is on the podium (1st to 3rd), which is highlighted with a stronger fill
   * and outline than the neutral treatment used from 4th place onward.
   */
  protected readonly isPodium = computed(() => this.position() <= 3);

  /**
   * Tailwind text color utility applied to the number and, through `currentColor`, to the
   * hexagon's fill and stroke.
   */
  protected readonly colorClass = computed(() => resolvePositionBadgeClass(this.position()));
}

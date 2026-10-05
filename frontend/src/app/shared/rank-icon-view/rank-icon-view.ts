import { Component, computed, input } from '@angular/core';
import { NgOptimizedImage } from '@angular/common';

import { RANK_ICON_PIXELS } from './rank-icon-view.constants';
import { RankIconSize } from './rank-icon-view.model';

/**
 * Competitive tier icon from an SVG in `public/ranks/`.
 */
@Component({
  selector: 'app-rank-icon-view',
  imports: [NgOptimizedImage],
  templateUrl: './rank-icon-view.html',
  host: { class: 'contents' },
})
export class RankIconView {
  /**
   * Icon path from `resolveCompetitiveTierIconUrl()`, `null` renders nothing.
   */
  public readonly src = input<string | null>(null);

  /**
   * Translated tier name, used as alt text.
   */
  public readonly tierLabel = input.required<string>();

  /**
   * Size preset: sm 32px, md 48px, lg 64px.
   */
  public readonly size = input<RankIconSize>('md');

  /**
   * Side of the current size in pixels.
   */
  protected readonly pixels = computed(() => RANK_ICON_PIXELS[this.size()]);
}

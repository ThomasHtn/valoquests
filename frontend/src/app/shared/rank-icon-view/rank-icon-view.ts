import { Component, computed, input } from '@angular/core';
import { NgOptimizedImage } from '@angular/common';

import { RANK_ICON_SIZES } from './rank-icon-view.constants';
import { RankIconSize } from './rank-icon-view.model';

/**
 * Competitive rank tier icon component, rendered from an SVG asset in `public/ranks/`.
 *
 * Displays a single SVG badge representing the player's competitive tier (Iron, Gold, Radiant,
 * etc.), paired with an optional tier label shown in nearby text. Uses `NgOptimizedImage` for
 * lazy loading and responsive optimization; the SVG path is resolved separately by the consuming
 * component via `resolveCompetitiveTierIconUrl()`.
 *
 * Reusable across player lists, profile pages, and any tier-display context, with size presets
 * (sm, md, lg) to match layout density and visual hierarchy.
 */
@Component({
  selector: 'app-rank-icon-view',
  imports: [NgOptimizedImage],
  templateUrl: './rank-icon-view.html',
  host: { class: 'contents' },
})
export class RankIconView {
  /**
   * SVG file path in the public folder (e.g., `/ranks/gold-2.svg`), resolved by the consumer
   * using `resolveCompetitiveTierIconUrl()`. If `null`, no icon is rendered.
   */
  public readonly src = input<string | null>(null);

  /**
   * Tier name for the icon's alt text (e.g., `"Gold 2"`), translated by the consumer.
   * Used for screen-reader accessibility and fallback text if the image fails to load.
   */
  public readonly tierLabel = input.required<string>();

  /**
   * Size preset controlling the icon's dimensions: sm (32px), md (48px), lg (64px).
   */
  public readonly size = input<RankIconSize>('md');

  /**
   * Rendering metrics matching the current {@link size}: CSS class, icon dimensions in pixels.
   */
  protected readonly metrics = computed(() => RANK_ICON_SIZES[this.size()]);
}

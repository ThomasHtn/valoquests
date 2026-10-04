import { NgOptimizedImage } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { LucideUser } from '@lucide/angular';

import { AVATAR_SIZES } from './avatar.constants';
import { AvatarSize } from './avatar.model';

/**
 * Round player portrait with a user-icon fallback; empty `alt` since the name sits beside it.
 */
@Component({
  selector: 'app-avatar',
  imports: [LucideUser, NgOptimizedImage],
  templateUrl: './avatar.html',
  host: { class: 'contents' },
})
export class Avatar {
  /**
   * Portrait URL, `null` for the fallback icon.
   */
  public readonly src = input<string | null>(null);

  /**
   * Size preset.
   */
  public readonly size = input<AvatarSize>('md');

  /**
   * Fetches eagerly; only for the page's LCP image, or `NgOptimizedImage` logs `NG02955`.
   */
  public readonly priority = input(false);

  /**
   * Draws the reigning weekly Champion's gold ring.
   */
  public readonly champion = input(false);

  /**
   * Metrics of the current {@link size}.
   */
  protected readonly metrics = computed(() => AVATAR_SIZES[this.size()]);

  /**
   * Disc shared by the portrait and the fallback.
   */
  protected readonly frameClass = computed(() => `${this.metrics().containerClass} rounded-full`);
}

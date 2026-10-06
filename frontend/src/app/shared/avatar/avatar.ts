import { NgOptimizedImage } from '@angular/common';
import { Component, computed, input } from '@angular/core';

import { LucideUser } from '@lucide/angular';

import { AVATAR_PIXELS } from './avatar.constants';
import { AvatarSize } from './avatar.model';

/**
 * Round player portrait with a user-icon fallback; empty `alt` since the name sits beside it.
 */
@Component({
  selector: 'app-avatar',
  imports: [LucideUser, NgOptimizedImage],
  templateUrl: './avatar.html',
  styleUrl: './avatar.scss',
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
   * Side of the current {@link size} in pixels.
   */
  protected readonly pixels = computed(() => AVATAR_PIXELS[this.size()]);

  /**
   * Size modifier of the disc.
   */
  protected readonly sizeClass = computed(() => `avatar--${this.size()}`);
}

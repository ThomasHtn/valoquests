import { NgOptimizedImage } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { LucideUser } from '@lucide/angular';

import { AVATAR_SIZES } from './avatar.constants';
import { AvatarSize } from './avatar.model';

/**
 * Player avatar, falling back to a generic user icon when no portrait is available.
 *
 * Shared by every screen listing or featuring a player so they all render the exact same
 * placeholder for players without a resolved portrait.
 *
 * The portrait is decorative: every call site renders the player's name as text beside it, so an
 * empty `alt` keeps screen readers from announcing the same player twice.
 */
@Component({
  selector: 'app-avatar',
  imports: [LucideUser, NgOptimizedImage],
  templateUrl: './avatar.html',
  host: { class: 'contents' },
})
export class Avatar {
  /**
   * Resolved portrait URL, or `null` to render the fallback icon.
   */
  public readonly src = input<string | null>(null);

  /**
   * Size preset controlling the container, the fallback icon and the image's intrinsic dimensions.
   */
  public readonly size = input<AvatarSize>('md');

  /**
   * Rendering metrics matching the current {@link size}.
   */
  protected readonly metrics = computed(() => AVATAR_SIZES[this.size()]);
}

import { NgOptimizedImage } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { LucideUser } from '@lucide/angular';

import { AVATAR_HEX_CLASS, AVATAR_SIZES } from './avatar.constants';
import { AvatarShape, AvatarSize } from './avatar.model';

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
   * Silhouette the portrait is cut to. Hexagons mark a player who holds a rank — see
   * {@link AvatarShape}.
   */
  public readonly shape = input<AvatarShape>('circle');

  /**
   * Whether the portrait should be fetched eagerly, ahead of the rest of the page.
   *
   * Enable it on the single avatar that is the page's largest contentful paint (today, the profile
   * header portrait). `NgOptimizedImage` otherwise logs `NG02955` and the browser discovers the
   * image late. Left off by default: marking several images as priority defeats the purpose.
   */
  public readonly priority = input(false);

  /**
   * Whether this avatar belongs to the reigning weekly "Champion" (see {@link ChampionBadge}),
   * drawing a gold ring around it so the title reads at a glance wherever the player's name
   * appears, not just next to the badge itself.
   */
  public readonly champion = input(false);

  /**
   * Rendering metrics matching the current {@link size}.
   */
  protected readonly metrics = computed(() => AVATAR_SIZES[this.size()]);

  /**
   * Container utilities for the current {@link size} and {@link shape}, applied to the portrait
   * and to the fallback alike so both are cut to the same silhouette.
   */
  protected readonly frameClass = computed(() => {
    const { containerClass, roundedClass } = this.metrics();
    const shape = this.shape();

    return `${containerClass} ${shape === 'hex' ? AVATAR_HEX_CLASS : roundedClass}`;
  });
}

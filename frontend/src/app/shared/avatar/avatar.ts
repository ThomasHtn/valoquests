import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { LucideUser } from '@lucide/angular';

import { AVATAR_SIZE_CLASSES } from './avatar.constants';
import { AvatarSize } from './avatar.model';

/**
 * Player avatar, falling back to a generic user icon when no portrait is available.
 *
 * Shared by every screen listing or featuring a player so they all render the exact same
 * placeholder for players without a resolved portrait.
 */
@Component({
  selector: 'app-avatar',
  imports: [LucideUser],
  templateUrl: './avatar.html',
  host: { class: 'contents' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Avatar {
  /**
   * Resolved portrait URL, or `null` to render the fallback icon.
   */
  public readonly src = input<string | null>(null);

  /**
   * Size preset controlling both the container and fallback-icon dimensions.
   */
  public readonly size = input<AvatarSize>('md');

  /**
   * Tailwind classes matching the current {@link size}.
   */
  protected readonly sizeClasses = computed(() => AVATAR_SIZE_CLASSES[this.size()]);
}

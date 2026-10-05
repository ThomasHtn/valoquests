import { NgOptimizedImage } from '@angular/common';
import { Component, computed, input } from '@angular/core';

/**
 * Square map or agent thumbnail with a monogram fallback; hidden from AT without a name.
 */
@Component({
  selector: 'app-media-thumbnail',
  imports: [NgOptimizedImage],
  templateUrl: './media-thumbnail.html',
  styleUrl: './media-thumbnail.scss',
  host: { class: 'contents' },
})
export class MediaThumbnail {
  /**
   * Image URL, `null` for the monogram fallback.
   */
  public readonly src = input<string | null>(null);

  /**
   * Fallback letter shown when {@link src} is `null`.
   */
  public readonly monogram = input.required<string>();

  /**
   * Accessible name, `null` when decorative.
   */
  public readonly accessibleName = input<string | null>(null);

  /**
   * Whether the top-right corner is cut.
   */
  public readonly notched = input(true);

  /**
   * Whether assistive technology skips the thumbnail, as it has no name.
   */
  protected readonly decorative = computed(() => this.accessibleName() === null);
}

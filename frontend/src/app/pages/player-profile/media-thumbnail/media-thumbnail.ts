import { NgOptimizedImage } from '@angular/common';
import { Component, input } from '@angular/core';

/**
 * Square, notched thumbnail for a match's map or agent: the resolved portrait when one exists,
 * a monogram fallback otherwise. Written out identically in both the table and the card layout
 * of the match history, so it is kept in one place instead of twice.
 *
 * Map thumbnails are decorative — the map's name is already shown as text beside them — while
 * an agent portrait is the only thing naming the agent in the card layout, so it needs a real
 * accessible name. {@link accessibleName} carries that distinction: left unset, both the image
 * and its fallback stay hidden from assistive technology; set, the image gets it as `alt` and
 * the fallback gets it as a visually-hidden span next to the monogram.
 */
@Component({
  selector: 'app-media-thumbnail',
  imports: [NgOptimizedImage],
  templateUrl: './media-thumbnail.html',
  host: { class: 'contents' },
})
export class MediaThumbnail {
  /**
   * Resolved portrait URL, or `null` to render the monogram fallback.
   */
  public readonly src = input<string | null>(null);

  /**
   * Fallback letter shown when {@link src} is `null`.
   */
  public readonly monogram = input.required<string>();

  /**
   * Accessible name for the thumbnail, or `null` when it is purely decorative.
   */
  public readonly accessibleName = input<string | null>(null);
}

import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { EmptyIllustration as EmptyIllustrationKind } from './empty-plate.model';

/**
 * The line drawing of an empty plate.
 *
 * Drawn in the base scene's own idiom rather than taken from an illustration pack: solid strokes
 * are what exists, dotted strokes are what remains to be done, amber marks the one thing the
 * reader is waiting on. A flat-colour pack would sit on this ground like a sticker.
 *
 * Hand-written SVG, so it stays a few hundred bytes per drawing and takes the theme's colours.
 */
@Component({
  selector: 'app-empty-illustration',
  templateUrl: './empty-illustration.html',
  styleUrl: './empty-illustration.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block', 'aria-hidden': 'true' },
})
export class EmptyIllustration {
  /**
   * Which of the four drawings to render.
   */
  public readonly kind = input.required<EmptyIllustrationKind>();
}

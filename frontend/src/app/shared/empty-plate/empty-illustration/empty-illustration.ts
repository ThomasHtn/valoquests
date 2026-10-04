import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { EmptyIllustration as EmptyIllustrationKind } from '../empty-plate.model';

/**
 * Hand-written SVG drawing of an empty plate, in the base scene's line idiom.
 */
@Component({
  selector: 'app-empty-illustration',
  templateUrl: './empty-illustration.html',
  styleUrl: './empty-illustration.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block', 'aria-hidden': 'true' },
})
export class EmptyIllustration {
  /**
   * Drawing to render.
   */
  public readonly kind = input.required<EmptyIllustrationKind>();
}

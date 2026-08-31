import { Component, input } from '@angular/core';

import { ColonyTierGlyph } from '@core/colony/colony-view.model';

/**
 * The silhouette one step of the ladder wears — a camp, houses, a skyline, a monument.
 *
 * `tierGlyphFor` has sorted the twelve steps into these four bands since the ladder was written, but
 * nothing drew them: the step's marker carried its position instead, which is the one thing the
 * marker's own colour and fill already say. Drawing the band is what lets a locked step be worth
 * wanting — a reader sees the shape of what the materials buy, not a number they do not have yet.
 *
 * Sized by the caller through a plain `class` on the host (`size-5`, `size-7`) and coloured by
 * `currentColor`, like every other icon in the interface.
 */
@Component({
  selector: 'app-tier-glyph',
  templateUrl: './tier-glyph.html',
  host: { class: 'block', 'aria-hidden': 'true' },
})
export class TierGlyph {
  /**
   * Which of the four bands to draw.
   */
  public readonly glyph = input.required<ColonyTierGlyph>();
}

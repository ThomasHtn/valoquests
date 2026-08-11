import { Component, output } from '@angular/core';

import { TranslatePipe } from '@core/i18n/translate-pipe';

/**
 * Compass call to action of the landing page.
 *
 * The landing page's only affordance, so it is a real `<button>` rather than a clickable box:
 * pointer and keyboard activation, focus ring and accessible name all come for free that way. Its
 * accessible name is its own visible wording, as WCAG 2.5.3 requires; every ring, tick, needle and
 * ripple around it is decoration and is hidden from assistive technologies.
 *
 * Split out of the page for the same reason the overview splits its own sections: this is forty
 * nodes of pure ornament that would otherwise bury the page's actual content.
 */
@Component({
  selector: 'app-compass',
  imports: [TranslatePipe],
  templateUrl: './compass.html',
  host: { class: 'block' },
})
export class Compass {
  /**
   * Emitted when the visitor activates the compass, by pointer or by keyboard.
   */
  public readonly entered = output<void>();
}

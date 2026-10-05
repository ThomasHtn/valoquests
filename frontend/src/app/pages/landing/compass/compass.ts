import { Component, output } from '@angular/core';

import { TranslatePipe } from '@core/i18n/translate-pipe';

/**
 * Landing call to action: a real button named by its visible label (WCAG 2.5.3).
 */
@Component({
  selector: 'app-compass',
  imports: [TranslatePipe],
  templateUrl: './compass.html',
  styleUrl: './compass.scss',
})
export class Compass {
  /**
   * Emitted when the visitor activates the compass.
   */
  public readonly entered = output<void>();
}

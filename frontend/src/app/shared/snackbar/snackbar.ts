import { Component, inject } from '@angular/core';
import { LucideCircleCheck, LucideTriangleAlert, LucideX } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { SNACKBAR_DURATION_MS, SnackbarService } from '@core/snackbar/snackbar';

/**
 * Application-wide snackbar, mounted once at the root so every page shares the same single slot.
 *
 * Bottom-anchored and self-dismissing with a timebar, replacing what used to be a per-page inline
 * success/error line: an operator no longer has to look back at the control they clicked to learn
 * what happened, and the message no longer competes for space with the rest of the page.
 */
@Component({
  selector: 'app-snackbar',
  imports: [TranslatePipe, LucideCircleCheck, LucideTriangleAlert, LucideX],
  templateUrl: './snackbar.html',
  host: { class: 'contents' },
})
export class Snackbar {
  /**
   * Service holding the current and queued snackbars.
   */
  protected readonly snackbar = inject(SnackbarService);

  /**
   * Duration the timebar animation must match exactly, in milliseconds.
   */
  protected readonly snackbarDurationMs = SNACKBAR_DURATION_MS;
}

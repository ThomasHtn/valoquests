import { Component, inject } from '@angular/core';
import { LucideCircleCheck, LucideTriangleAlert, LucideX } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { SnackbarService } from '@core/snackbar/snackbar';
import {
  SNACKBAR_DURATION_MS,
  SNACKBAR_ERROR_DURATION_MS,
} from '@core/snackbar/snackbar.constants';

/**
 * Application-wide snackbar, mounted once at the root; self-dismissing with a timebar.
 */
@Component({
  selector: 'app-snackbar',
  imports: [TranslatePipe, LucideCircleCheck, LucideTriangleAlert, LucideX],
  templateUrl: './snackbar.html',
  host: { class: 'contents' },
})
export class Snackbar {
  /**
   * Current and queued snackbars.
   */
  protected readonly snackbar = inject(SnackbarService);

  /**
   * Timebar duration in ms, matching the service exactly.
   */
  protected readonly snackbarDurationMs = SNACKBAR_DURATION_MS;

  /**
   * Timebar duration of an error in ms, longer so it can be read.
   */
  protected readonly errorDurationMs = SNACKBAR_ERROR_DURATION_MS;
}

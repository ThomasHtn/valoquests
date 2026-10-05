import { Component, computed, inject } from '@angular/core';
import { LucideCircleCheck, LucideTriangleAlert, LucideX } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { SnackbarQueue } from '@core/snackbar/snackbar';
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
  styleUrl: './snackbar.scss',
  host: { class: 'contents' },
})
export class Snackbar {
  /**
   * Current and queued snackbars.
   */
  protected readonly snackbar = inject(SnackbarQueue);

  /**
   * Text of a current success, announced politely; empty otherwise.
   */
  protected readonly politeText = computed(() => {
    const message = this.snackbar.current();

    return message?.type === 'success' ? message.text : '';
  });

  /**
   * Text of a current error, announced at once; empty otherwise.
   */
  protected readonly assertiveText = computed(() => {
    const message = this.snackbar.current();

    return message?.type === 'error' ? message.text : '';
  });

  /**
   * Timebar duration in ms, matching the service exactly; an error stays longer so it can be read.
   */
  protected readonly timebarDurationMs = computed(() =>
    this.snackbar.current()?.type === 'error' ? SNACKBAR_ERROR_DURATION_MS : SNACKBAR_DURATION_MS,
  );
}

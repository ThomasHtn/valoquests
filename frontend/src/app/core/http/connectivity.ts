import { DOCUMENT, inject, Service, signal } from '@angular/core';

import { SnackbarService } from '@core/snackbar/snackbar';
import { Translation } from '@core/i18n/translation';

/**
 * Tracks whether the device is online and tells the reader when that changes.
 *
 * Mostly read on a phone, on mobile data: a lost connection must say so once, in plain words,
 * rather than surface as every screen failing one after the other.
 */
@Service()
export class Connectivity {
  private readonly window = inject(DOCUMENT).defaultView;
  private readonly snackbar = inject(SnackbarService);
  private readonly translation = inject(Translation);

  /**
   * Whether the browser currently reports a network connection.
   */
  public readonly online = signal(this.window?.navigator.onLine ?? true);

  constructor() {
    this.window?.addEventListener('offline', () => {
      this.online.set(false);
      this.snackbar.error(this.translation.translate('connectivity.offline'));
    });
    this.window?.addEventListener('online', () => {
      this.online.set(true);
      this.snackbar.success(this.translation.translate('connectivity.online'));
    });
  }
}

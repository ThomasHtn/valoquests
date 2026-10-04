import { DOCUMENT, inject, Service, signal } from '@angular/core';

import { SnackbarService } from '@core/snackbar/snackbar';
import { Translation } from '@core/i18n/translation';

/**
 * Tracks the online state and announces each change once, rather than every screen failing.
 */
@Service()
export class Connectivity {
  /**
   * Browser window whose online and offline events drive the state.
   */
  private readonly window = inject(DOCUMENT).defaultView;

  /**
   * Snackbar queue, to announce each connectivity change once.
   */
  private readonly snackbar = inject(SnackbarService);

  /**
   * Translation service, to word the connectivity announcements.
   */
  private readonly translation = inject(Translation);

  /**
   * Whether the browser reports a network connection.
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

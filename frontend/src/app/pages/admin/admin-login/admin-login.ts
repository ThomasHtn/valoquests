import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { LucideChevronLeft, LucideEye, LucideEyeOff, LucideLockKeyhole } from '@lucide/angular';

import { AdminApi } from '@core/admin/admin-api';
import { resolveAdminErrorMessage } from '@core/admin/admin-error.utils';
import { AdminSession } from '@core/admin/session/admin-session';
import { ADMIN_HOME_ROUTE } from '@core/admin/session/admin-session.constants';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { SnackbarQueue } from '@core/snackbar/snackbar';
import { Button } from '@shared/button/button';
import { Spinner } from '@shared/spinner/spinner';
import { TextField } from '@shared/text-field/text-field';
import { TextFieldInput } from '@shared/text-field/text-field-input';

/**
 * Chrome-free backoffice sign-in, reached by URL only.
 * The key is checked against `GET /api/admin/session` first, so the session never holds a bad key.
 */
@Component({
  selector: 'app-admin-login',
  imports: [
    TranslatePipe,
    RouterLink,
    Button,
    TextField,
    TextFieldInput,
    LucideChevronLeft,
    LucideEye,
    LucideEyeOff,
    LucideLockKeyhole,
    Spinner,
  ],
  templateUrl: './admin-login.html',
  styleUrl: './admin-login.scss',
  host: { class: 'ambient-field' },
})
export class AdminLogin {
  /**
   * Backoffice API, to check the key before it is stored.
   */
  private readonly adminApi = inject(AdminApi);

  /**
   * Admin session, which keeps the accepted key.
   */
  private readonly session = inject(AdminSession);

  /**
   * Router, to enter the backoffice once signed in.
   */
  private readonly router = inject(Router);

  /**
   * Translation, for the fallback rejection message.
   */
  private readonly translation = inject(Translation);

  /**
   * Snackbar, echoing a rejection outside the form.
   */
  private readonly snackbar = inject(SnackbarQueue);

  /**
   * Typed key.
   */
  protected readonly key = signal('');

  /**
   * Whether a verification request is in flight.
   */
  protected readonly verifying = signal(false);

  /**
   * Translated failure message, `''` when none.
   */
  protected readonly error = signal('');

  /**
   * Whether the key is shown in plain text.
   */
  protected readonly showKey = signal(false);

  /**
   * Whether a non-blank key can be sent, never twice at once.
   */
  protected readonly canSubmit = computed(() => !this.verifying() && this.key().trim() !== '');

  /**
   * Records the typed key and clears the error.
   */
  protected onKeyInput(event: Event): void {
    this.key.set((event.target as HTMLInputElement).value);
    this.error.set('');
  }

  /**
   * Toggles key visibility.
   */
  protected toggleShowKey(): void {
    this.showKey.update((current) => !current);
  }

  /**
   * Verifies the key and enters the backoffice when the backend accepts it.
   */
  protected async submit(event: Event): Promise<void> {
    event.preventDefault();

    const candidate = this.key().trim();

    if (candidate === '' || this.verifying()) {
      return;
    }

    this.verifying.set(true);
    this.error.set('');

    try {
      await this.adminApi.verifyKey(candidate);
      this.session.signIn(candidate);
      await this.router.navigate([ADMIN_HOME_ROUTE]);
    } catch (error: unknown) {
      const message = resolveAdminErrorMessage(
        error,
        this.translation.translate('admin.login.rejected'),
      );

      this.error.set(message);
      this.snackbar.error(message);
    } finally {
      this.verifying.set(false);
    }
  }
}

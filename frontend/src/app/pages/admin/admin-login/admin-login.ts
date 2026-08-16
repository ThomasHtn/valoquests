import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import {
  LucideEye,
  LucideEyeOff,
  LucideLoaderCircle,
  LucideLockKeyhole,
  LucideTriangleAlert,
} from '@lucide/angular';

import { AdminApi } from '@core/admin/admin-api';
import { resolveAdminErrorMessage } from '@core/admin/admin-error.utils';
import { ADMIN_HOME_ROUTE } from '@core/admin/admin-session.constants';
import { AdminSession } from '@core/admin/admin-session';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Button } from '@shared/button/button';

/**
 * Backoffice sign-in screen.
 *
 * Reached by URL only — nothing in the application links here — and rendered chrome-free like the
 * landing page, since a sidebar offering the public navigation would contradict what this screen
 * is for.
 *
 * The key is verified against `GET /api/admin/session` before the session opens. That route does
 * nothing but answer, so a wrong key costs a rejected request rather than a half-applied operation,
 * and the session never holds a key the backend would refuse on the first real command.
 */
@Component({
  selector: 'app-admin-login',
  imports: [
    TranslatePipe,
    Button,
    LucideEye,
    LucideEyeOff,
    LucideLoaderCircle,
    LucideLockKeyhole,
    LucideTriangleAlert,
  ],
  templateUrl: './admin-login.html',
  // Diverges from `PAGE_LAYOUT_CLASS`: this is a single centred composition filling the viewport,
  // not a stack of blocks inside the application shell.
  host: { class: 'flex min-h-dvh items-center justify-center bg-surface-sunken px-4 py-10' },
})
export class AdminLogin {
  /**
   * Data-access service used to verify the key.
   */
  private readonly adminApi = inject(AdminApi);

  /**
   * Session opened once the backend accepts the key.
   */
  private readonly session = inject(AdminSession);

  /**
   * Router used to enter the backoffice on success.
   */
  private readonly router = inject(Router);

  /**
   * i18n service used to resolve the fallback failure message.
   */
  private readonly translation = inject(Translation);

  /**
   * Key currently typed in the field.
   */
  protected readonly key = signal('');

  /**
   * Whether a verification request is in flight.
   */
  protected readonly verifying = signal(false);

  /**
   * Already-translated failure message, or `''` when nothing has failed yet.
   */
  protected readonly error = signal('');

  /**
   * Whether the key field currently reveals its typed value in plain text.
   */
  protected readonly showKey = signal(false);

  /**
   * Records what the operator typed into the key field.
   *
   * @param event - The input event carrying the field's current value.
   */
  protected onKeyInput(event: Event): void {
    this.key.set((event.target as HTMLInputElement).value);
    this.error.set('');
  }

  /**
   * Toggles whether the key field reveals its typed value.
   */
  protected toggleShowKey(): void {
    this.showKey.update((current) => !current);
  }

  /**
   * Verifies the typed key and enters the backoffice when the backend accepts it.
   *
   * @param event - The form submission, whose default navigation is prevented.
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
      this.error.set(
        resolveAdminErrorMessage(error, this.translation.translate('admin.login.rejected')),
      );
    } finally {
      this.verifying.set(false);
    }
  }
}

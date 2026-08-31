import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import {
  provideRouter,
  TitleStrategy,
  withComponentInputBinding,
  withViewTransitions,
} from '@angular/router';

import { adminKeyInterceptor } from '@core/admin/admin-key.interceptor';
import { TranslatedTitleStrategy } from '@core/i18n/translated-title-strategy';
import { Translation } from '@core/i18n/translation';
import { routes } from './app.routes';

/**
 * Root application configuration.
 *
 * Registers global error listeners, the router, the HTTP client and an app
 * initializer that loads the initial translation dictionary before the UI renders.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // View transitions make the application read as one surface instead of eleven documents: the
    // wordmark, the rail and the context bar hold still while the body underneath is replaced. The
    // animation itself lives in `styles.css`, which names `page-body` so only that column moves —
    // and drops out entirely under `prefers-reduced-motion`. The first paint is skipped: there is
    // nothing to transition *from* when the application opens.
    provideRouter(
      routes,
      withComponentInputBinding(),
      withViewTransitions({ skipInitialTransition: true }),
    ),
    // `withFetch` selects the fetch-based backend over the legacy XHR one, which this zoneless
    // application has no reason to keep. The interceptor only touches `/api/admin` requests, to
    // which it adds the administrator key.
    provideHttpClient(withFetch(), withInterceptors([adminKeyInterceptor])),
    { provide: TitleStrategy, useClass: TranslatedTitleStrategy },
    provideAppInitializer(() => inject(Translation).initialize()),
  ],
};

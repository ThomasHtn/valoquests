import { provideHttpClient, withFetch } from '@angular/common/http';
import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter, TitleStrategy, withComponentInputBinding } from '@angular/router';

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
    provideRouter(routes, withComponentInputBinding()),
    // `withFetch` selects the fetch-based backend over the legacy XHR one, which this zoneless
    // application has no reason to keep.
    provideHttpClient(withFetch()),
    { provide: TitleStrategy, useClass: TranslatedTitleStrategy },
    provideAppInitializer(() => inject(Translation).initialize()),
  ],
};

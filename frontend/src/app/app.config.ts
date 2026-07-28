import { provideHttpClient } from '@angular/common/http';
import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './app.routes';
import { Translation } from './core/i18n/translation';

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
    provideHttpClient(),
    provideAppInitializer(() => inject(Translation).initialize()),
  ],
};

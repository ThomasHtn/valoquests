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

import { adminKeyInterceptor } from '@core/admin/session/admin-key.interceptor';
import { TranslatedTitleStrategy } from '@core/i18n/title-strategy/translated-title-strategy';
import { Translation } from '@core/i18n/translation';

import { routes } from './app.routes';

/**
 * Root configuration; the initializer loads the translation dictionary before the first render.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // Only `page-body` animates (see `styles/page-layout.css`); nothing to transition from on open.
    provideRouter(
      routes,
      withComponentInputBinding(),
      withViewTransitions({ skipInitialTransition: true }),
    ),
    // The interceptor adds the admin key to `/api/admin` requests only.
    provideHttpClient(withFetch(), withInterceptors([adminKeyInterceptor])),
    { provide: TitleStrategy, useClass: TranslatedTitleStrategy },
    provideAppInitializer(() => inject(Translation).initialize()),
  ],
};

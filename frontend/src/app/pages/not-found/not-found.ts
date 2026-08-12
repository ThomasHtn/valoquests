import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { TranslatePipe } from '@core/i18n/translate-pipe';

/**
 * Page rendered for any URL that matches no route.
 *
 * Offers a way back to the overview rather than leaving the user on an empty shell.
 */
@Component({
  selector: 'app-not-found',
  imports: [TranslatePipe, RouterLink],
  template: `
    <section class="flex min-h-[60vh] flex-col items-start justify-center">
      <p class="tracking-label-wide font-mono text-2xs font-medium text-brand-500 uppercase">404</p>
      <h1
        class="font-display mt-2 text-2xl leading-none font-bold text-text-primary uppercase sm:text-3xl"
      >
        {{ 'notFound.title' | translate }}
      </h1>
      <p class="mt-2 max-w-prose text-lead text-text-secondary">
        {{ 'notFound.description' | translate }}
      </p>
      <a
        class="notch-tr notch-tr-edge focus-ring-inset mt-6 inline-flex min-h-10 items-center border border-surface-700 bg-surface-900 px-4 py-2 text-sm font-semibold text-text-primary transition-colors [--notch:0.5rem] hover:border-surface-600 hover:bg-surface-800 motion-safe:active:scale-[0.96]"
        routerLink="/overview"
      >
        {{ 'notFound.backToOverview' | translate }}
      </a>
    </section>
  `,
})
export class NotFound {}

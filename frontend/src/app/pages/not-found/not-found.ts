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
    <section class="flex flex-col items-start gap-3 py-12">
      <p class="font-display text-6xl font-bold text-brand-500">404</p>
      <h1 class="text-2xl font-bold tracking-wide text-text-primary uppercase">
        {{ 'notFound.title' | translate }}
      </h1>
      <p class="text-sm text-text-secondary">{{ 'notFound.description' | translate }}</p>
      <a
        class="focus-ring mt-2 inline-flex min-h-10 items-center rounded-lg border border-surface-700 px-4 py-2 text-sm font-semibold text-text-primary transition-colors hover:bg-surface-800"
        routerLink="/overview"
      >
        {{ 'notFound.backToOverview' | translate }}
      </a>
    </section>
  `,
})
export class NotFound {}

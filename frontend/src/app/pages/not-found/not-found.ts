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
        class="focus-ring tracking-label mt-6 inline-flex h-12 items-center border border-text-primary/15 px-4 font-mono text-xs font-medium text-text-muted uppercase transition-colors hover:bg-brand-500/8 hover:text-text-primary motion-safe:active:scale-[0.96]"
        routerLink="/overview"
      >
        {{ 'notFound.backToOverview' | translate }}
      </a>
    </section>
  `,
})
export class NotFound {}

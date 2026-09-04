import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { TourVisit } from '@core/tour/tour-visit';

/**
 * Placeholder for the guided tour, rewritten around the rescue mission by lot 11 of the redesign.
 *
 * Marks the visit as completed on arrival so the entry guard hands the next visit straight to the
 * overview, the way the real tour does at its last step.
 */
@Component({
  selector: 'app-tour',
  imports: [TranslatePipe, RouterLink],
  template: `
    <main class="flex min-h-dvh flex-col items-center justify-center gap-6 bg-surface-950 px-6">
      <p class="max-w-prose text-center text-lead text-text-secondary">
        {{ 'wip.description' | translate: { lot: 11 } }}
      </p>
      <a
        class="focus-ring tracking-label inline-flex h-12 items-center border border-edge px-4 font-mono text-xs font-medium text-text-muted uppercase transition-colors hover:bg-brand-500/8 hover:text-text-primary"
        routerLink="/overview"
      >
        {{ 'tour.finish' | translate }}
      </a>
    </main>
  `,
})
export class Tour {
  constructor() {
    inject(TourVisit).markCompleted();
  }
}

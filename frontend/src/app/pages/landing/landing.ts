import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { LandingVisit } from '@core/landing/landing-visit';
import { Compass } from './compass/compass';

/**
 * Landing page.
 *
 * The doorway into the application, shown on a first visit at the root route and skipped from then
 * on by `landingEntryGuard`. Unlike every other page it renders outside `Shell`, without navigation
 * chrome: its single affordance is the compass, so any competing control would work against it.
 */
@Component({
  selector: 'app-landing',
  imports: [TranslatePipe, Compass],
  templateUrl: './landing.html',
  styleUrl: './landing.css',
  // Diverges from `PAGE_LAYOUT_CLASS`: this page is not a stack of blocks inside the application
  // shell but a single full-viewport composition, laid out by its own template. `block` only
  // avoids the default inline display of a custom element.
  host: { class: 'block' },
})
export class Landing {
  /**
   * Records the entry so that returning visitors go straight to the overview.
   */
  private readonly landingVisit = inject(LandingVisit);

  /**
   * Navigates on to the guided tour once the compass has been activated.
   */
  private readonly router = inject(Router);

  /**
   * Records the entry and moves on to the guided tour, which hands the visitor over to the overview
   * once it is done — or straight away, for a visitor who has already been through it.
   */
  protected enter(): void {
    this.landingVisit.markEntered();
    void this.router.navigate(['/tour']);
  }
}

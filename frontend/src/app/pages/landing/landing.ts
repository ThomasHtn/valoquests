import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { LandingVisit } from '@core/landing/landing-visit';

import { Compass } from './compass/compass';
import { LANDING_MARKS } from './landing.constants';

/**
 * First-visit landing page, chrome-free so the compass is its only control.
 */
@Component({
  selector: 'app-landing',
  imports: [TranslatePipe, Compass],
  templateUrl: './landing.html',
  styleUrl: './landing.scss',
  // Not `PAGE_LAYOUT_CLASS`: a single full-viewport composition outside the shell.
  host: { class: 'block' },
})
export class Landing {
  /**
   * Figures on the horizon.
   */
  protected readonly marks = LANDING_MARKS;

  /**
   * Landing visit record, so returning visitors skip the landing.
   */
  private readonly landingVisit = inject(LandingVisit);

  /**
   * Router, to open the tour once the visitor enters.
   */
  private readonly router = inject(Router);

  /**
   * Records the entry so returning visitors skip the landing, then opens the tour.
   */
  protected enter(): void {
    this.landingVisit.markEntered();
    void this.router.navigate(['/tour'], { replaceUrl: true });
  }
}

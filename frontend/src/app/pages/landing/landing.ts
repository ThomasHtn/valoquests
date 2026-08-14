import { Component, computed, inject } from '@angular/core';
import { Router } from '@angular/router';

import { ChallengesApi } from '@core/challenges/challenges-api';
import { isoWeekNumber } from '@core/date/week-period.utils';
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
   * Data-access service backing the shared current-challenges resource, read here only for the
   * active week's boundaries.
   */
  private readonly challengesApi = inject(ChallengesApi);

  /**
   * Records the entry so that returning visitors go straight to the overview.
   */
  private readonly landingVisit = inject(LandingVisit);

  /**
   * Navigates on to the guided tour once the compass has been activated.
   */
  private readonly router = inject(Router);

  /**
   * Active week's ISO number, or `null` while it is unknown.
   *
   * Derived from the same shared resource the overview reads, so showing it here costs no extra
   * request. `null` is a legitimate state rather than an error — the week label simply drops its
   * number — which is why this is a bare `@if` in the template instead of an
   * `<app-resource-state>`: a spinner-and-retry block would be out of proportion for a caption
   * whose absence blocks nothing.
   */
  protected readonly weekNumber = computed<number | null>(() => {
    const currentChallenges = this.challengesApi.current;
    return currentChallenges.hasValue() ? isoWeekNumber(currentChallenges.value().weekStart) : null;
  });

  /**
   * Records the entry and moves on to the guided tour, which hands the visitor over to the overview
   * once it is done — or straight away, for a visitor who has already been through it.
   */
  protected enter(): void {
    this.landingVisit.markEntered();
    void this.router.navigate(['/tour']);
  }
}

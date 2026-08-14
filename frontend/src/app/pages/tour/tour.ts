import { Component, computed, ElementRef, inject, signal, viewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { LucideChevronLeft, LucideChevronRight } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { TourVisit } from '@core/tour/tour-visit';
import { BossEncounter } from '@pages/overview/boss-encounter/boss-encounter';
import { Podium } from '@pages/overview/podium/podium';
import { TeamProgress } from '@pages/overview/team-progress/team-progress';

import { PlayersPreview } from './players-preview/players-preview';
import { TOUR_STEPS } from './tour.constants';
import { TourStepId } from './tour.model';

/**
 * Guided tour.
 *
 * The briefing a first-time visitor gets between the landing page and the overview: five steps
 * covering what the week is about, each pairing a couple of sentences with the very component the
 * visitor is about to meet. It stays at the level of the broad strokes on purpose — `/rules` is the
 * reference for the numbers, and the closing step points there.
 *
 * Like `Landing`, and unlike every other page, it renders outside `Shell`: navigation chrome would
 * invite the visitor to wander off mid-briefing, which is exactly what the tour exists to postpone.
 * `tourEntryGuard` keeps it to a single showing, with the `?replay` escape hatch behind the rules
 * page's replay link.
 *
 * The illustrations are the real components, reading the same shared resources as the overview
 * rather than fixtures of their own: whatever the visitor sees here is what they will find one
 * click later, and the tour cannot drift as those components evolve. The trade is that a slow or
 * unreachable backend surfaces their loading and error states inside the tour — acceptable, since
 * each of them already handles those states in full.
 */
@Component({
  selector: 'app-tour',
  imports: [
    TranslatePipe,
    RouterLink,
    LucideChevronLeft,
    LucideChevronRight,
    BossEncounter,
    TeamProgress,
    Podium,
    PlayersPreview,
  ],
  templateUrl: './tour.html',
  // Diverges from `PAGE_LAYOUT_CLASS`, same rationale as `Landing`: this is a full-viewport
  // composition under the root outlet, not a stack of blocks inside the application shell.
  host: {
    class: 'block',
    '(document:keydown)': 'onKeydown($event)',
  },
})
export class Tour {
  /**
   * Records the completion so that the tour steps aside on subsequent visits.
   */
  private readonly tourVisit = inject(TourVisit);

  /**
   * Navigates into the application once the tour is over or skipped.
   */
  private readonly router = inject(Router);

  /**
   * The scrolling content region, reset to its top on every step change so a step never opens
   * halfway down the previous one's scroll position.
   */
  private readonly content = viewChild.required<ElementRef<HTMLElement>>('content');

  /**
   * Index of the step currently on screen.
   */
  private readonly stepIndex = signal(0);

  /**
   * The tour's steps, in order.
   */
  protected readonly steps = TOUR_STEPS;

  /**
   * Identifier of the step currently on screen, driving both its copy and its illustration.
   */
  protected readonly currentStep = computed<TourStepId>(() => this.steps[this.stepIndex()]);

  /**
   * Human-facing position of the current step, counting from 1.
   */
  protected readonly currentPosition = computed(() => this.stepIndex() + 1);

  /**
   * Whether the first step is on screen, where there is nothing to go back to.
   */
  protected readonly isFirst = computed(() => this.stepIndex() === 0);

  /**
   * Whether the last step is on screen, where "next" becomes "enter the application".
   */
  protected readonly isLast = computed(() => this.stepIndex() === this.steps.length - 1);

  /**
   * Whether the current step is illustrated by a component, which the opening step is not.
   *
   * Drives the step's layout: an illustrated step splits into copy and illustration side by side on
   * wide viewports, while the opening one is a single centered block.
   */
  protected readonly hasIllustration = computed(() => this.currentStep() !== 'intro');

  /**
   * The current step, as a single-item list.
   *
   * Iterating over it with `track` is what makes Angular tear the step's blocks down and build them
   * back up when the step changes, which is in turn what replays their entry animation — a plain
   * binding would keep the same elements and the animation would only ever run once, on arrival.
   * Kept as a `computed` rather than an inline array literal so the identity only changes when the
   * step does, instead of on every change detection.
   */
  protected readonly stepFrames = computed<readonly TourStepId[]>(() => [this.currentStep()]);

  /**
   * One flag per step, `true` for the steps already reached — the fill state of the progress bar's
   * segments.
   */
  protected readonly segments = computed<readonly boolean[]>(() =>
    this.steps.map((_, index) => index <= this.stepIndex()),
  );

  /**
   * Moves on to the next step, or into the application from the last one.
   */
  protected next(): void {
    if (this.isLast()) {
      this.finish();
      return;
    }

    this.stepIndex.update((index) => index + 1);
    this.resetScroll();
  }

  /**
   * Goes back to the previous step, if there is one.
   */
  protected previous(): void {
    if (this.isFirst()) {
      return;
    }

    this.stepIndex.update((index) => index - 1);
    this.resetScroll();
  }

  /**
   * Leaves the tour early. Records the completion just like walking it through does: a visitor who
   * skipped it asked not to see it, and the rules page keeps a way back to it.
   */
  protected skip(): void {
    this.finish();
  }

  /**
   * Records the completion without navigating, for the closing step's link to the rules page: the
   * `routerLink` carries the navigation, so triggering {@link finish} here would race it and land
   * the visitor on the overview instead.
   */
  protected leaveForRules(): void {
    this.tourVisit.markCompleted();
  }

  /**
   * Records the completion and moves on to the overview.
   */
  private finish(): void {
    this.tourVisit.markCompleted();
    void this.router.navigate(['/overview']);
  }

  /**
   * Keyboard shortcuts covering the on-screen controls: the arrow keys walk the tour, `Escape`
   * leaves it. Bound on the document rather than on the buttons so they work wherever the focus
   * sits, including inside the illustrations.
   *
   * Ignored while a modifier is held, so browser and OS shortcuts keep their meaning.
   *
   * @param event - The keyboard event to interpret.
   */
  protected onKeydown(event: KeyboardEvent): void {
    if (event.altKey || event.ctrlKey || event.metaKey) {
      return;
    }

    switch (event.key) {
      case 'ArrowRight':
        this.next();
        break;
      case 'ArrowLeft':
        this.previous();
        break;
      case 'Escape':
        this.skip();
        break;
      default:
        return;
    }

    event.preventDefault();
  }

  /**
   * Scrolls the content region back to its top, after a step change.
   */
  private resetScroll(): void {
    this.content().nativeElement.scrollTo({ top: 0 });
  }
}

import { Component, computed, ElementRef, inject, signal, viewChild } from '@angular/core';
import { Router } from '@angular/router';
import { LucideChevronLeft, LucideChevronRight, LucideUsers } from '@lucide/angular';

import { ColonyView } from '@core/colony/colony-view';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { TourVisit } from '@core/tour/tour-visit';
import { Podium } from '@pages/overview/podium/podium';
import { TownSilhouette } from '@pages/overview/town-silhouette/town-silhouette';
import { BossBand } from '@pages/week/boss-band/boss-band';
import { CountUp } from '@shared/count-up/count-up';
import { LadderStrip } from '@shared/ladder-strip/ladder-strip';
import { NavChip } from '@shared/nav-chip/nav-chip';
import { TierGlyph } from '@shared/tier-glyph/tier-glyph';

import { PlayerSpotlight } from './player-spotlight/player-spotlight';
import { TOUR_EMPHASIS_MARKER, TOUR_SPEC_KEYS, TOUR_STEPS } from './tour.constants';
import { TourStepId } from './tour.model';

/**
 * Guided tour.
 *
 * The briefing a first-time visitor gets between the landing page and the overview: six steps
 * covering what the squad is playing for, each pairing a couple of sentences with the very
 * component the visitor is about to meet. It stays at the level of the broad strokes on purpose —
 * `/rules` is the reference for the numbers, and the closing step points there.
 *
 * It is written for someone who has not been recruited: a curious visitor, a coach, a player
 * passing through. The copy therefore describes what the squad does rather than handing the reader
 * a set of instructions, and only turns to "what you would do here" once the tour is over.
 *
 * Like `Landing`, and unlike every other page, it renders outside `Shell`: navigation chrome would
 * invite the visitor to wander off mid-briefing, which is exactly what the tour exists to postpone.
 * `tourEntryGuard` keeps it to a single showing, with the `?replay` escape hatch behind the rules
 * page's replay link.
 *
 * The illustrations are the real components the pages themselves render, reading the same shared
 * resources: whatever the visitor sees here is what they will find one click later, and the tour
 * cannot drift as those components evolve. That contract had quietly broken — the tour was the last
 * caller of three panels the redesigned overview had dropped, so it was illustrating an application
 * that no longer existed. The trade is that a slow or unreachable backend surfaces their loading
 * and error states inside the tour — acceptable, since each of them already handles those in full.
 */
@Component({
  selector: 'app-tour',
  imports: [
    TranslatePipe,
    LucideChevronLeft,
    LucideChevronRight,
    LucideUsers,
    TownSilhouette,
    BossBand,
    LadderStrip,
    Podium,
    PlayerSpotlight,
    NavChip,
    CountUp,
    TierGlyph,
  ],
  templateUrl: './tour.html',
  styleUrl: './tour.css',
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
   * The squad's colony, read for the three figures the town scene is drawn from. Bound exactly as
   * `/overview` binds them, so the tour opens on the very picture waiting one click later.
   */
  protected readonly colony = inject(ColonyView);

  /**
   * The step of the ladder the colony stands on, which names the population figure beside it.
   *
   * The same lookup `/overview` makes for its own readout: the ladder carries the state, and the
   * current step is the one entry of it a reader needs here.
   */
  protected readonly currentTier = computed(() =>
    this.colony.ladder().find((tier) => tier.state === 'CURRENT'),
  );

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
   * Translation sub-keys of the three figures closing every step.
   */
  protected readonly specKeys = TOUR_SPEC_KEYS;

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
   */
  protected readonly hasIllustration = computed(() => this.currentStep() !== 'intro');

  /**
   * Whether the step's illustration needs the whole column rather than the half a side-by-side
   * layout leaves it.
   *
   * The boss band is the one component here built for a full page width: below `62rem` it drops to
   * a single column and its health readout stacks into four short lines. Beside a column of copy it
   * never clears that breakpoint, so this step puts the copy above it instead and hands it the
   * width its own page gives it.
   */
  protected readonly isWideIllustration = computed(() => this.currentStep() === 'week');

  /**
   * Whether the opening step is on screen, where the colony is drawn as a horizon under the copy
   * rather than as a panel beside it.
   *
   * The step used to be a block of prose centered on an empty gradient — the one screen of the six
   * with nothing to look at, and the first one a visitor meets. It now opens on the squad's own
   * town, low and wide under the headline, which is both the product and the only thing on this
   * page that could belong to no other product.
   */
  protected readonly isHero = computed(() => this.currentStep() === 'intro');

  /**
   * How the step arranges its copy and what illustrates it.
   *
   * Three layouts, resolved here rather than as nested ternaries in the template: the opening step
   * and the week's boss band both centre their copy over a full-width illustration, and every other
   * step reads as copy beside illustration from `lg` up.
   */
  protected readonly frameClass = computed(() =>
    this.isHero() || this.isWideIllustration()
      ? 'flex flex-col items-center gap-8'
      : 'grid items-center gap-10 lg:grid-cols-[minmax(0,27rem)_minmax(0,1fr)] lg:gap-16',
  );

  /**
   * The copy column: ranged left beside its illustration on wide viewports, centered whenever it
   * stands over one instead.
   */
  protected readonly copyClass = computed(() =>
    this.hasIllustration() && !this.isWideIllustration()
      ? 'items-center text-center lg:items-start lg:text-left'
      : 'max-w-4xl items-center text-center',
  );

  /**
   * The claim's measure: the narrower one for a step sharing its row with an illustration, the
   * wider for a step that has the whole width to itself — held to the narrow measure, that one left
   * a ribbon of text stranded in the middle of the viewport.
   *
   * In `rem` rather than `ch`. The claim steps up a size at `sm`, and `ch` is resolved against the
   * element's own font-size, so the same measure silently widened at the breakpoint — the column
   * moved when only the type was supposed to.
   */
  protected readonly bodyClass = computed(() =>
    this.hasIllustration() && !this.isWideIllustration() ? 'max-w-[17rem]' : 'max-w-[28rem]',
  );

  /**
   * Size of the step's heading.
   *
   * The opening step is set a full step larger than the rest: it has no illustration to share the
   * screen with, and it is the one line that has to make a visitor want to walk the other five.
   */
  protected readonly titleClass = computed(() =>
    this.hasIllustration()
      ? 'text-[2.125rem]/[1.06] sm:text-5xl/[1.06]'
      : 'text-5xl/[1.04] sm:text-6xl/[1.04] lg:text-7xl/[1.04]',
  );

  /**
   * The scrolling band's own vertical rhythm.
   *
   * A stacked step is the tallest of the six — a full-width band under its own copy rather than
   * beside it — and it is the only one the page's usual padding pushed past the fold. Tightened
   * rather than left to scroll: the step is one picture and one explanation, and a visitor should
   * not have to reach for the illustration the sentence above is pointing at.
   */
  protected readonly contentClass = computed(() =>
    this.isWideIllustration() || this.isHero() ? 'py-6 sm:py-8' : 'py-10 sm:py-16',
  );

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
   * Splits a step's translated claim into plain and emphasized runs.
   *
   * The claim is one sentence with a couple of words set in relief, marked in the dictionary as
   * `*so*` (see {@link TOUR_EMPHASIS_MARKER}). Splitting on the marker leaves the emphasized runs
   * at the odd indices, which is what the template keys its styling off.
   *
   * Runs carry their own leading and trailing spaces, and the template renders them as adjacent
   * elements: Angular strips the whitespace-only nodes between those elements, so the spacing a
   * reader sees is the spacing the sentence was written with, never the template's indentation.
   *
   * @param claim - The step's translated claim.
   * @returns The claim's runs, in order, each flagged for emphasis.
   */
  protected claimRuns(claim: string): readonly { text: string; strong: boolean }[] {
    return claim
      .split(TOUR_EMPHASIS_MARKER)
      .map((text, index) => ({ text, strong: index % 2 === 1 }))
      .filter((run) => run.text.length > 0);
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

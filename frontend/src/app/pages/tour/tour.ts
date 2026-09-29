import { Component, computed, ElementRef, inject, signal, viewChild } from '@angular/core';
import { Router } from '@angular/router';
import { LucideChevronLeft, LucideChevronRight } from '@lucide/angular';

import { resolveDifficultyVisual } from '@core/challenges/challenge-visual.utils';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { TourVisit } from '@core/tour/tour-visit';
import { Breakpoint } from '@core/viewport/breakpoint';
import { ChallengeCardView } from '@pages/challenges/challenge-card/challenge-card';
import { ChallengeCard } from '@pages/challenges/challenges.model';
import { buildRungs } from '@pages/challenges/challenges.utils';
import { Podium } from '@pages/leaderboard/podium/podium';
import { BaseScene } from '@pages/overview/base-scene/base-scene';
import { MissionReadings } from '@pages/overview/mission-readings/mission-readings';
import { Mission } from '@pages/overview/overview.model';
import { CountUp } from '@shared/count-up/count-up';
import { NavChip } from '@shared/nav-chip/nav-chip';

import { TourCapacity } from './tour-capacity/tour-capacity';
import {
  FULL_CAMPAIGN_POPULATION,
  TOUR_EMPHASIS_MARKER,
  TOUR_SPEC_KEYS,
  TOUR_STEP_SOURCES,
  TOUR_STEPS,
} from './tour.constants';
import { TourStepId } from './tour.model';
import {
  TOUR_SAMPLE_CAPACITY,
  TOUR_SAMPLE_CHALLENGES,
  TOUR_SAMPLE_CONTRIBUTION,
  TOUR_SAMPLE_DEADLINE_IN_MS,
  TOUR_SAMPLE_MATCHES,
  TOUR_SAMPLE_MISSION,
  TOUR_SAMPLE_OPERATORS,
  TOUR_SAMPLE_PODIUM,
  TOUR_SAMPLE_POPULATION,
  TOUR_SAMPLE_POPULATION_CHANGE,
  TOUR_SAMPLE_STAGES_DONE,
} from './tour-samples.constants';
import { TourTracker } from './tour-tracker/tour-tracker';

/**
 * Guided tour.
 *
 * The briefing a first-time visitor gets between the landing page and the overview: six steps
 * covering what the squad is playing for, each pairing one claim and three figures with a reading
 * of the screen it stands for. It stays at the level of the broad strokes on purpose; `/rules` is
 * the reference for the numbers, and the closing step points there.
 *
 * Like `Landing`, it renders outside `Shell`: navigation chrome would invite the visitor to
 * wander off mid-briefing. `tourEntryGuard` keeps it to a single showing, with the `?replay`
 * escape hatch behind the rules page's replay link.
 *
 * The illustrations are the pages' own components, fed a fixed sample campaign: the live one is
 * empty between two campaigns and thin on a Monday, and a briefing has to show the game playing.
 */
@Component({
  selector: 'app-tour',
  imports: [
    TranslatePipe,
    LucideChevronLeft,
    LucideChevronRight,
    BaseScene,
    MissionReadings,
    ChallengeCardView,
    Podium,
    CountUp,
    NavChip,
    TourTracker,
    TourCapacity,
  ],
  templateUrl: './tour.html',
  styleUrl: './tour.scss',
  // Diverges from `PAGE_LAYOUT_CLASS`, same rationale as `Landing`: a full-viewport composition
  // under the root outlet, not a stack of blocks inside the application shell.
  host: {
    class: 'block',
    '(document:keydown)': 'onKeydown($event)',
  },
})
export class Tour {
  private readonly tourVisit = inject(TourVisit);

  private readonly router = inject(Router);

  private readonly translation = inject(Translation);

  /**
   * The scrolling content region, reset to its top on every step change.
   */
  private readonly content = viewChild.required<ElementRef<HTMLElement>>('content');

  private readonly stepIndex = signal(0);

  protected readonly steps = TOUR_STEPS;

  protected readonly specKeys = TOUR_SPEC_KEYS;

  protected readonly sources = TOUR_STEP_SOURCES;

  protected readonly fullCampaignPopulation = FULL_CAMPAIGN_POPULATION;

  protected readonly currentStep = computed<TourStepId>(() => this.steps[this.stepIndex()]);

  protected readonly currentPosition = computed(() => this.stepIndex() + 1);

  protected readonly isFirst = computed(() => this.stepIndex() === 0);

  protected readonly isLast = computed(() => this.stepIndex() === this.steps.length - 1);

  /**
   * The current step, as a single-item list: iterating it with `track` is what makes Angular tear
   * the step's blocks down and build them back up when the step changes, which replays their entry
   * animation.
   */
  protected readonly stepFrames = computed<readonly TourStepId[]>(() => [this.currentStep()]);

  /**
   * One flag per step, `true` for the steps already reached.
   */
  protected readonly segments = computed<readonly boolean[]>(() =>
    this.steps.map((_, index) => index <= this.stepIndex()),
  );

  /**
   * Whether the viewport holds the week's clock too: below `md`, the report drops it so the step
   * fits a phone without scrolling.
   */
  protected readonly isMedium = inject(Breakpoint).isMedium;

  protected readonly sampleMatches = TOUR_SAMPLE_MATCHES;

  protected readonly samplePopulation = TOUR_SAMPLE_POPULATION;

  protected readonly samplePopulationChange = TOUR_SAMPLE_POPULATION_CHANGE;

  protected readonly sampleStagesDone = TOUR_SAMPLE_STAGES_DONE;

  protected readonly sampleCapacity = TOUR_SAMPLE_CAPACITY;

  protected readonly sampleContribution = TOUR_SAMPLE_CONTRIBUTION;

  protected readonly samplePodium = TOUR_SAMPLE_PODIUM;

  /**
   * The sample week, its countdown set from the clock so it always reads as a Friday evening.
   */
  protected readonly sampleMission: Mission = {
    ...TOUR_SAMPLE_MISSION,
    extractionDeadline: Date.now() + TOUR_SAMPLE_DEADLINE_IN_MS,
  };

  /**
   * The sample week's challenges, worded as the challenges page words its own.
   */
  protected readonly sampleChallenges = computed<readonly ChallengeCard[]>(() =>
    TOUR_SAMPLE_CHALLENGES.map((challenge) => {
      const visual = resolveDifficultyVisual(challenge.difficulty);
      const rungs = buildRungs(
        TOUR_SAMPLE_OPERATORS,
        challenge.target,
        (playerId) => {
          const value = challenge.progress[playerId - 1] ?? 0;
          return { value, done: value >= challenge.target };
        },
        (amount) => String(amount),
      );
      return {
        tone: visual.tierColor,
        mark: visual.tier,
        kind: this.translation.translate(`common.challengeDifficulty.${challenge.difficulty}`),
        name: this.translation.translate(`tour.samples.challenges.${challenge.key}.name`),
        description: this.translation.translate(
          `tour.samples.challenges.${challenge.key}.description`,
        ),
        survivors: challenge.survivors,
        rankingPoints: 0,
        rescueActive: true,
        target: challenge.target,
        rungs,
        doneCount: rungs.filter((rung) => rung.done).length,
      };
    }),
  );

  /**
   * Splits a step's translated claim into plain and emphasized runs, marked `*so*` in the
   * dictionary. Runs carry their own spaces and the template renders them as adjacent elements.
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

  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }

  protected signed(amount: number): string {
    const sign = amount > 0 ? '+' : amount < 0 ? '−' : '';
    return `${sign}${this.format(Math.abs(amount))}`;
  }

  protected next(): void {
    if (this.isLast()) {
      this.finish();
      return;
    }

    this.stepIndex.update((index) => index + 1);
    this.resetScroll();
  }

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
   * Keyboard shortcuts covering the on-screen controls: the arrow keys walk the tour, `Escape`
   * leaves it. Ignored while a modifier is held, so browser and OS shortcuts keep their meaning.
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

  private finish(): void {
    this.tourVisit.markCompleted();
    void this.router.navigate(['/overview']);
  }

  private resetScroll(): void {
    this.content().nativeElement.scrollTo({ top: 0 });
  }
}

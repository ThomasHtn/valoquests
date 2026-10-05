import { Component, computed, ElementRef, inject, signal, viewChild } from '@angular/core';
import { Router } from '@angular/router';
import { LucideChevronLeft, LucideChevronRight } from '@lucide/angular';

import { resolveLocale } from '@core/i18n/format/locale.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { TourVisit } from '@core/tour/tour-visit';
import { Breakpoint } from '@core/viewport/breakpoint';
import { BoardRow } from '@core/challenges/card/challenge-card.model';
import { DayCell } from '@pages/challenges/challenges.model';
import { DailyWeek } from '@pages/challenges/daily-week/daily-week';
import { DeckCard } from '@shared/deck-card/deck-card';
import { Podium } from '@pages/leaderboard/podium/podium';
import { MissionReadings } from '@pages/overview/mission-readings/mission-readings';
import { Mission } from '@pages/overview/mission-readings/mission-readings.model';
import { NavChip } from '@shared/nav-chip/nav-chip';

import { TourBasePreview } from './tour-base-preview/tour-base-preview';
import { TourCampaignTrack } from './tour-campaign-track/tour-campaign-track';
import { TourCapacity } from './tour-capacity/tour-capacity';
import {
  FULL_CAMPAIGN_POPULATION,
  TOUR_SPEC_KEYS,
  TOUR_STEP_SOURCES,
  TOUR_STEPS,
  TOUR_STEPS_WITHOUT_SPECS,
} from './tour.constants';
import { ClaimRun, TourStepId } from './tour.model';
import {
  TOUR_SAMPLE_CAPACITY,
  TOUR_SAMPLE_CONTRIBUTION,
  TOUR_SAMPLE_DAILY,
  TOUR_SAMPLE_DAILY_TALLY,
  TOUR_SAMPLE_DEADLINE_IN_MS,
  TOUR_SAMPLE_MATCHES,
  TOUR_SAMPLE_MISSION,
  TOUR_SAMPLE_OPERATORS,
  TOUR_SAMPLE_PODIUM,
  TOUR_SAMPLE_POPULATION,
  TOUR_SAMPLE_STAGES_DONE,
  TOUR_SAMPLE_STAKES,
} from './tour-samples.constants';
import { TourTracker } from './tour-tracker/tour-tracker';
import {
  buildTourDailyRow,
  buildTourWeek,
  endOfDay,
  splitEmphasis,
  startOfWeek,
} from './tour.utils';

/**
 * Chrome-free first-visit tour; real components fed a sample, the live campaign may be empty.
 */
@Component({
  selector: 'app-tour',
  imports: [
    TranslatePipe,
    LucideChevronLeft,
    LucideChevronRight,
    MissionReadings,
    DeckCard,
    DailyWeek,
    Podium,
    NavChip,
    TourTracker,
    TourCapacity,
    TourBasePreview,
    TourCampaignTrack,
  ],
  templateUrl: './tour.html',
  styleUrl: './tour.scss',
  // Not `PAGE_LAYOUT_CLASS`: a full-viewport composition outside the shell.
  host: {
    class: 'block',
    '(document:keydown)': 'onKeydown($event)',
  },
})
export class Tour {
  /**
   * Tour visit record, marked completed on leaving so the tour shows once.
   */
  private readonly tourVisit = inject(TourVisit);

  /**
   * Router, to land on the overview once the tour ends.
   */
  private readonly router = inject(Router);

  /**
   * Translation, to build the sample daily row and week in the current language.
   */
  private readonly translation = inject(Translation);

  /**
   * Scrolling content region, reset to its top on every step change.
   */
  private readonly content = viewChild.required<ElementRef<HTMLElement>>('content');

  /**
   * Zero-based index of the shown step.
   */
  private readonly stepIndex = signal(0);

  /**
   * Tour steps in display order.
   */
  protected readonly steps = TOUR_STEPS;

  /**
   * Translation sub-keys of the figures closing a step.
   */
  protected readonly specKeys = TOUR_SPEC_KEYS;

  /**
   * Title key of the page each step's illustration comes from.
   */
  protected readonly sources = TOUR_STEP_SOURCES;

  /**
   * Population of a full campaign, the scale of the base preview.
   */
  protected readonly fullCampaignPopulation = FULL_CAMPAIGN_POPULATION;

  /**
   * Identifier of the shown step, which picks its illustration and copy.
   */
  protected readonly currentStep = computed<TourStepId>(() => this.steps[this.stepIndex()]);

  /**
   * One-based position of the shown step, for the counter.
   */
  protected readonly currentPosition = computed(() => this.stepIndex() + 1);

  /**
   * Whether the first step is shown, which disables the back button.
   */
  protected readonly isFirst = computed(() => this.stepIndex() === 0);

  /**
   * Whether the last step is shown, where next finishes the tour.
   */
  protected readonly isLast = computed(() => this.stepIndex() === this.steps.length - 1);

  /**
   * Whether the shown step closes on its three figures.
   */
  protected readonly hasSpecs = computed(
    () => !TOUR_STEPS_WITHOUT_SPECS.includes(this.currentStep()),
  );

  /**
   * The shown step's claim, split into plain and emphasized runs.
   */
  protected readonly claimRuns = computed<readonly ClaimRun[]>(() =>
    splitEmphasis(this.translation.translate(`tour.steps.${this.currentStep()}.claim`)),
  );

  /**
   * Current step as a one-item list: `track` rebuilds the blocks and replays their entry.
   */
  protected readonly stepFrames = computed<readonly TourStepId[]>(() => [this.currentStep()]);

  /**
   * One flag per step, `true` for the steps already reached.
   */
  protected readonly segments = computed<readonly boolean[]>(() =>
    this.steps.map((_, index) => index <= this.stepIndex()),
  );

  /**
   * Whether the week's clock fits; below `md` it is dropped so a step fits a phone.
   */
  protected readonly isMedium = inject(Breakpoint).isMedium;

  /**
   * Sample evening of matches for the tracker step.
   */
  protected readonly sampleMatches = TOUR_SAMPLE_MATCHES;

  /**
   * Sample population the base preview opens on.
   */
  protected readonly samplePopulation = TOUR_SAMPLE_POPULATION;

  /**
   * Sample rocket stages, for the base preview and the campaign track.
   */
  protected readonly sampleStagesDone = TOUR_SAMPLE_STAGES_DONE;

  /**
   * Sample dials for the resources step.
   */
  protected readonly sampleCapacity = TOUR_SAMPLE_CAPACITY;

  /**
   * Sample squad damage for the mission readings.
   */
  protected readonly sampleContribution = TOUR_SAMPLE_CONTRIBUTION;

  /**
   * Sample Sunday stakes for the mission readings.
   */
  protected readonly sampleStakes = TOUR_SAMPLE_STAKES;

  /**
   * Sample leaders for the ranking step.
   */
  protected readonly samplePodium = TOUR_SAMPLE_PODIUM;

  /**
   * Sample week, its countdown set from the clock to always read as a Friday evening.
   */
  protected readonly sampleMission: Mission = {
    ...TOUR_SAMPLE_MISSION,
    extractionDeadline: Date.now() + TOUR_SAMPLE_DEADLINE_IN_MS,
  };

  /**
   * Sample daily challenge, still running until the end of the visitor's day.
   */
  protected readonly sampleDailyRow = computed<BoardRow>(() => {
    return buildTourDailyRow(
      TOUR_SAMPLE_DAILY,
      TOUR_SAMPLE_OPERATORS,
      endOfDay(Date.now()),
      this.translation.language(),
      (key, params) => this.translation.translate(key, params),
    );
  });

  /**
   * The sample week's seven days, today on the sample Friday.
   */
  protected readonly sampleDays = computed<readonly DayCell[]>(() =>
    buildTourWeek(
      TOUR_SAMPLE_DAILY_TALLY,
      this.sampleDailyRow().doneCount,
      TOUR_SAMPLE_OPERATORS.length,
      startOfWeek(Date.now()),
      resolveLocale(this.translation.language()),
      (key, params) => this.translation.translate(key, params),
    ),
  );

  /**
   * The day the strip picks: today, the one after the tallied days.
   */
  protected readonly sampleToday = TOUR_SAMPLE_DAILY_TALLY.length;

  /**
   * Moves to the next step, or finishes the tour on the last one.
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
   * Moves back one step, ignored on the first one.
   */
  protected previous(): void {
    if (this.isFirst()) {
      return;
    }

    this.stepIndex.update((index) => index - 1);
    this.resetScroll();
  }

  /**
   * Arrows walk the tour, `Escape` leaves; skipped with modifiers or a focused input.
   */
  protected onKeydown(event: KeyboardEvent): void {
    if (
      event.altKey ||
      event.ctrlKey ||
      event.metaKey ||
      event.target instanceof HTMLInputElement
    ) {
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
        this.finish();
        break;
      default:
        return;
    }

    event.preventDefault();
  }

  /**
   * Records the tour as completed, even when skipped, and replaces it with the overview.
   */
  protected finish(): void {
    this.tourVisit.markCompleted();
    void this.router.navigate(['/overview'], { replaceUrl: true });
  }

  /**
   * Brings the new step's content back to its top.
   */
  private resetScroll(): void {
    this.content().nativeElement.scrollTo({ top: 0 });
  }
}

import { Component, computed, ElementRef, inject, signal, viewChild } from '@angular/core';
import { Router } from '@angular/router';
import {
  LucideChevronLeft,
  LucideChevronRight,
  LucideHeartPulse,
  LucideSkull,
  LucideUsers,
  LucideWheat,
  LucideWrench,
  LucideZap,
} from '@lucide/angular';

import { CampaignApi } from '@core/campaign/campaign-api';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { RankingApi } from '@core/ranking/ranking-api';
import { TourVisit } from '@core/tour/tour-visit';
import { BaseScene } from '@pages/overview/base-scene/base-scene';
import { PlanetFigure } from '@pages/overview/planet-figure/planet-figure';
import { CountUp } from '@shared/count-up/count-up';
import { NavChip } from '@shared/nav-chip/nav-chip';

import {
  FULL_CAMPAIGN_POPULATION,
  TOUR_EMPHASIS_MARKER,
  TOUR_SPEC_KEYS,
  TOUR_STEPS,
} from './tour.constants';
import { TourStepId } from './tour.model';

/**
 * Guided tour.
 *
 * The briefing a first-time visitor gets between the landing page and the overview: six steps
 * covering what the squad is playing for, each pairing one claim and three figures with a reading
 * of the live campaign. It stays at the level of the broad strokes on purpose; `/rules` is the
 * reference for the numbers, and the closing step points there.
 *
 * Like `Landing`, it renders outside `Shell`: navigation chrome would invite the visitor to
 * wander off mid-briefing. `tourEntryGuard` keeps it to a single showing, with the `?replay`
 * escape hatch behind the rules page's replay link.
 *
 * The illustrations read the same shared resources the pages do, so whatever the visitor sees
 * here is what they will find one click later. Between two campaigns the readings are simply
 * absent and the copy stands alone.
 */
@Component({
  selector: 'app-tour',
  imports: [
    TranslatePipe,
    LucideChevronLeft,
    LucideChevronRight,
    LucideHeartPulse,
    LucideSkull,
    LucideUsers,
    LucideWheat,
    LucideWrench,
    LucideZap,
    BaseScene,
    PlanetFigure,
    CountUp,
    NavChip,
  ],
  templateUrl: './tour.html',
  styleUrl: './tour.css',
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

  private readonly campaignApi = inject(CampaignApi);

  private readonly challengesApi = inject(ChallengesApi);

  private readonly rankingApi = inject(RankingApi);

  /**
   * The scrolling content region, reset to its top on every step change.
   */
  private readonly content = viewChild.required<ElementRef<HTMLElement>>('content');

  private readonly stepIndex = signal(0);

  protected readonly steps = TOUR_STEPS;

  protected readonly specKeys = TOUR_SPEC_KEYS;

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
   * The live campaign, or `null` between two campaigns and before the resource resolves.
   */
  protected readonly campaign = computed(() => {
    const campaign = resourceValue(this.campaignApi.campaign, null);
    return campaign && campaign.status !== null ? campaign : null;
  });

  /**
   * The base as it stands, the hero's and the second step's reading.
   */
  protected readonly base = computed(() => this.campaign()?.base ?? null);

  protected readonly stagesDone = computed(() => this.campaign()?.totals?.guardiansDefeated ?? 0);

  /**
   * The week in progress, for the planet the third step draws.
   */
  protected readonly mission = computed(() => {
    const campaign = this.campaign();
    const index = campaign?.currentWeekIndex ?? null;
    const week = index === null ? null : (campaign?.weeks[index - 1] ?? null);
    if (!week || week.guardianHitPoints <= 0) {
      return null;
    }
    const left = Math.max(0, week.guardianHitPoints - week.damageDealt);
    return {
      planetName: week.planetName,
      guardianName: week.guardianName ?? '',
      guardianLeft: left / week.guardianHitPoints,
      progressPercent: week.progressPercent,
      woundedCount: week.woundedCount,
      defeated: week.defeated,
    };
  });

  /**
   * Today's challenge, the fifth step's reading, or `null` when none is drawn yet.
   */
  protected readonly daily = computed(() => {
    const current = resourceValue(this.challengesApi.current, null);
    return current?.dailies.find((daily) => daily.day === current.today) ?? null;
  });

  /**
   * The three operators leading the week, the last step's reading.
   */
  protected readonly podium = computed(() => {
    const ranking = resourceValue(this.rankingApi.current, null)?.ranking ?? [];
    return ranking.filter((entry) => entry.position !== null).slice(0, 3);
  });

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

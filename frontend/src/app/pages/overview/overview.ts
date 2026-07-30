import {
  afterNextRender,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  signal,
  viewChild,
  viewChildren,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';

import { ChallengesApi } from '@core/challenges/challenges-api';
import { formatDateRange, isoWeekNumber, remainingWeekTime } from '@core/date/week-period.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { COUNTDOWN_REFRESH_INTERVAL_MS } from './overview.constants';
import { WeekSummary } from './overview.model';
import { Podium } from './podium/podium';
import { TeamProgress } from './team-progress/team-progress';
import { WeeklyChallenges } from './weekly-challenges/weekly-challenges';
import { WeeklyRanking } from './weekly-ranking/weekly-ranking';

/**
 * Ratio of a snap section that must be visible before the dot rail's active state switches to it,
 * chosen so the indicator updates roughly mid-transition rather than at the very first sliver of
 * the next section coming into view.
 */
const SECTION_VISIBILITY_THRESHOLD = 0.55;

/**
 * One section of the overview's scroll-snap layout, paired with the translation key of its dot
 * rail label.
 */
interface OverviewSection {
  readonly id: string;
  readonly ariaLabelKey: string;
}

/**
 * Overview page.
 *
 * Landing page shown at the application root route, presented as three full-height, scroll-snapped
 * sections: a hero pairing the ranking podium with the team's collective progress toward the
 * week's challenges, then the weekly challenges card, then the weekly ranking card. A dot rail
 * lets desktop users jump directly to a section instead of scrolling through it.
 */
@Component({
  selector: 'app-overview',
  imports: [TranslatePipe, Podium, TeamProgress, WeeklyChallenges, WeeklyRanking],
  templateUrl: './overview.html',
  // Diverges from `PAGE_LAYOUT_CLASS`: every other page stacks its blocks in normal document flow,
  // but this page owns a single full-height scroll-snap container instead, so the shared
  // vertical-stack-with-gap treatment does not apply here. `relative` anchors the dot rail, which
  // is positioned against this host rather than the scroll container so it stays fixed in place
  // while the container beneath it scrolls.
  host: { class: 'relative block' },
})
export class Overview {
  /**
   * Data-access service backing the shared current-challenges resource, which also carries the
   * active week's boundaries used by the hero.
   */
  private readonly challengesApi = inject(ChallengesApi);

  /**
   * Used to stop observing sections when the page is navigated away from.
   */
  private readonly destroyRef = inject(DestroyRef);

  /**
   * Current time, refreshed periodically to keep the countdown display accurate.
   */
  private readonly now = signal(new Date());

  /**
   * Active week's number, date range and remaining time, or `null` while loading.
   *
   * Computed once here, from a single ticking clock, and passed down to `TeamProgress` so its
   * countdown never drifts from this value.
   */
  protected readonly week = computed<WeekSummary | null>(() => {
    const currentChallenges = this.challengesApi.current;
    if (!currentChallenges.hasValue()) {
      return null;
    }

    const currentWeek = currentChallenges.value();

    return {
      number: isoWeekNumber(currentWeek.weekStart),
      dateRange: formatDateRange(currentWeek.weekStart, currentWeek.weekEnd),
      remaining: remainingWeekTime(currentWeek.weekEnd, this.now()),
    };
  });

  /**
   * Sections of the scroll-snap layout, in scroll order, driving both the `@for` loop rendering
   * each `<section>` and the dot rail navigating between them.
   */
  protected readonly sections: readonly OverviewSection[] = [
    { id: 'overview-hero', ariaLabelKey: 'overview.sections.hero.ariaLabel' },
    { id: 'overview-challenges', ariaLabelKey: 'overview.sections.challenges.ariaLabel' },
    { id: 'overview-ranking', ariaLabelKey: 'overview.sections.ranking.ariaLabel' },
  ];

  /**
   * Id of the section currently in view, driving the dot rail's active indicator.
   */
  protected readonly activeSectionId = signal(this.sections[0].id);

  /**
   * The scroll-snap container, queried to serve as the `IntersectionObserver` root.
   */
  private readonly scrollContainer = viewChild.required<ElementRef<HTMLElement>>('scrollContainer');

  /**
   * Every rendered `<section>`, queried both to observe their visibility and to scroll to them
   * from the dot rail.
   */
  private readonly sectionElements = viewChildren<ElementRef<HTMLElement>>('sectionRef');

  /**
   * Refreshes {@link now} every minute so the countdown stays accurate for the lifetime of the
   * page, and starts observing which section is in view once the sections exist in the DOM.
   */
  constructor() {
    interval(COUNTDOWN_REFRESH_INTERVAL_MS)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.now.set(new Date()));

    // Deferred to after the first render: the sections it observes do not exist until the
    // template has rendered them, and `IntersectionObserver` is a browser-only API.
    afterNextRender(() => this.observeSections());
  }

  /**
   * Scrolls the section matching `id` into view, honoring `motion-safe:scroll-smooth` on the
   * scroll container rather than forcing an animation.
   *
   * @param id - Id of the target section, as declared in {@link sections}.
   */
  protected scrollToSection(id: string): void {
    this.sectionElements()
      .find((section) => section.nativeElement.id === id)
      ?.nativeElement.scrollIntoView({ block: 'start' });
  }

  /**
   * Observes every section's visibility within the scroll container, keeping
   * {@link activeSectionId} in sync with whichever one is currently in view.
   */
  private observeSections(): void {
    const observer = new IntersectionObserver(
      (entries) => {
        entries
          .filter((entry) => entry.isIntersecting)
          .forEach((entry) => this.activeSectionId.set(entry.target.id));
      },
      { root: this.scrollContainer().nativeElement, threshold: SECTION_VISIBILITY_THRESHOLD },
    );

    this.sectionElements().forEach((section) => observer.observe(section.nativeElement));
    this.destroyRef.onDestroy(() => observer.disconnect());
  }
}

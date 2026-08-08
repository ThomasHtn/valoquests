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
import { RouterLink } from '@angular/router';
import { LucideBookOpen, LucideChevronDown } from '@lucide/angular';
import { interval } from 'rxjs';

import { ChallengesApi } from '@core/challenges/challenges-api';
import { formatDateRange, isoWeekNumber, remainingWeekTime } from '@core/date/week-period.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { COUNTDOWN_REFRESH_INTERVAL_MS } from './overview.constants';
import { WeekSummary } from './overview.model';
import { BossEncounter } from './boss-encounter/boss-encounter';
import { Podium } from './podium/podium';
import { TeamProgress } from './team-progress/team-progress';
import { WeeklyChallenges } from './weekly-challenges/weekly-challenges';
import { WeeklyRanking } from './weekly-ranking/weekly-ranking';

/**
 * Intersection ratios at which the `IntersectionObserver` re-checks each section's visibility. A
 * single high ratio (e.g. 0.55) would never fire for a section taller than the scroll container —
 * its ratio could never climb that high — leaving {@link Overview.activeSectionId} stuck on
 * whichever section last crossed it. This dense step list instead makes the observer track every
 * section's ratio continuously, so the most-visible one can always be picked out.
 */
const SECTION_VISIBILITY_THRESHOLDS = Array.from({ length: 21 }, (_, step) => step / 20);

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
 * week's challenges and the weekly boss card, then the weekly challenges card, then the weekly
 * ranking card. A dot rail lets desktop users jump directly to a section instead of scrolling
 * through it.
 */
@Component({
  selector: 'app-overview',
  imports: [
    TranslatePipe,
    RouterLink,
    BossEncounter,
    Podium,
    TeamProgress,
    WeeklyChallenges,
    WeeklyRanking,
    LucideBookOpen,
    LucideChevronDown,
  ],
  templateUrl: './overview.html',
  // Diverges from `PAGE_LAYOUT_CLASS`: every other page stacks its blocks in normal document flow,
  // but this page owns a single full-height scroll-snap container instead, so the shared
  // vertical-stack-with-gap treatment does not apply here. The host itself stays unpositioned (see
  // `overview.html`): its own box is capped and padded by the ancestor route wrapper in `app.html`,
  // and everything this page renders escapes that box via `position: absolute` anchored to
  // `<main>` instead, so it is `block` only to avoid the default inline display of a custom
  // element.
  host: { class: 'block' },
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
   * Computed once here, from a single ticking clock, and passed down to `BossEncounter` and
   * `TeamProgress` so their countdown never drifts from this value.
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
   * Whether the hero section is the one currently in view. Drives both the bouncing "scroll down"
   * hint next to the dot rail and the "how it works" link floating over the hero, so neither
   * lingers once the visitor has scrolled past the hero.
   */
  protected readonly isHeroSectionActive = computed(
    () => this.activeSectionId() === this.sections[0].id,
  );

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
   * {@link activeSectionId} in sync with whichever one is currently the most visible — rather than
   * the last one to report any overlap at all, which could flip to a barely-entering neighbor
   * while the current section is still mostly on screen.
   */
  private observeSections(): void {
    const visibilityRatios = new Map<string, number>();

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => visibilityRatios.set(entry.target.id, entry.intersectionRatio));

        const [mostVisibleId] = [...visibilityRatios].reduce((mostVisible, candidate) =>
          candidate[1] > mostVisible[1] ? candidate : mostVisible,
        );
        this.activeSectionId.set(mostVisibleId);
      },
      { root: this.scrollContainer().nativeElement, threshold: SECTION_VISIBILITY_THRESHOLDS },
    );

    this.sectionElements().forEach((section) => observer.observe(section.nativeElement));
    this.destroyRef.onDestroy(() => observer.disconnect());
  }
}

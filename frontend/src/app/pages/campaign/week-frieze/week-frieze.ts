import {
  afterNextRender,
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  Injector,
  input,
  output,
  viewChild,
} from '@angular/core';
import { LucideCheck, LucideLock, LucideSkull, LucideX } from '@lucide/angular';

import { BossTimelineNode } from '@core/boss/boss-timeline.model';
import { resolveBossCategoryColorClass } from '@core/boss/boss-visual.utils';
import { BossCategory } from '@core/boss/boss.model';
import { ColonyBossView } from '@core/colony/colony-view.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { buildFriezeStars, FriezeStar, railSplitPercentage } from '../campaign-frieze.utils';

/**
 * One week of the run as the frieze lays it out: the timeline node, and what its fight was worth
 * the colony.
 */
export interface WeekFriezeEntry {
  readonly node: BossTimelineNode;
  readonly boss: ColonyBossView | null;
}

/**
 * How long the first marker waits before landing, and how far apart two of them are — the frieze
 * draws itself left to right once, which is the only motion the block owns.
 */
const LANDING_DELAY_SECONDS = 0.25;
const LANDING_STEP_SECONDS = 0.09;

/**
 * The run's ten weeks, hung off one straight rail as milestones.
 *
 * The rail is solid behind the week being fought and dotted ahead of it, and that week's marker is
 * the only one drawn larger — the two things that say where the squad stands without a caption.
 * Each settled week hangs a card off its own marker, alternating above and below the rail: cards
 * that alternate are events on a line, cards that line up are rows of a table, and this page has
 * already been a table once.
 */
@Component({
  selector: 'app-week-frieze',
  imports: [TranslatePipe, LucideCheck, LucideLock, LucideSkull, LucideX],
  templateUrl: './week-frieze.html',
  styleUrl: './week-frieze.css',
})
export class WeekFrieze {
  private readonly injector = inject(Injector);

  /**
   * The run's weeks, oldest first.
   */
  public readonly entries = input.required<readonly WeekFriezeEntry[]>();

  /**
   * Id of the week whose panel is unfolded under the frieze, or `null` while none is.
   */
  public readonly selectedId = input<string | null>(null);

  /**
   * Emitted when a marker is pressed — the page decides whether that opens, steps or closes.
   */
  public readonly selected = output<BossTimelineNode>();

  /**
   * Where the rail stops being solid, as a share of its own width: the centre of the marker being
   * fought, or the end of the road once every week has settled.
   */
  protected readonly railSplitPercentage = computed(() => {
    const entries = this.entries();

    return railSplitPercentage(
      entries.findIndex((entry) => entry.node.status === 'current'),
      entries.length,
    );
  });

  /**
   * The night ground behind the frieze, drawn once: a fixed field, so the stars never move between
   * two renders of the same page.
   */
  protected readonly stars: readonly FriezeStar[] = buildFriezeStars();

  /**
   * The scrolling box the frieze rides in, below the width where all ten weeks fit.
   */
  private readonly scroller = viewChild.required<ElementRef<HTMLElement>>('scroller');

  /**
   * Whether the frieze has already been scrolled to the week being fought.
   *
   * Once only: this positions the block at the marker a reader came for, and re-running it on any
   * later change would yank the frieze back from wherever they had scrolled it to.
   */
  private hasCentred = false;

  constructor() {
    // The weeks arrive from two resources, so the markers are laid out a tick after the component
    // is created; the effect waits for them, then hands the measuring to the render phase.
    effect(() => {
      if (this.entries().length > 0 && !this.hasCentred) {
        this.hasCentred = true;
        afterNextRender(() => this.centreOnCurrentWeek(), { injector: this.injector });
      }
    });
  }

  /**
   * Opens the frieze on the week being fought rather than on week one, wherever the block is too
   * narrow to hold all ten — that marker is what a reader came to the page for.
   */
  private centreOnCurrentWeek(): void {
    const box = this.scroller().nativeElement;
    const current = box.querySelector<HTMLElement>('.fw--now');

    if (current === null || box.scrollWidth <= box.clientWidth) {
      return;
    }

    box.scrollLeft = current.offsetLeft + current.offsetWidth / 2 - box.clientWidth / 2;
  }

  /**
   * When one marker lands, so the ten arrive left to right rather than at once.
   *
   * @param index - Position of the week in the run.
   * @returns The delay, as a CSS duration.
   */
  protected landingDelay(index: number): string {
    return `${(LANDING_DELAY_SECONDS + index * LANDING_STEP_SECONDS).toFixed(2)}s`;
  }

  /**
   * Colour of a settled week's morale figure: a fight that was lost cost morale, and a cost is not
   * printed in the same ink as a gain.
   *
   * @param boss - What the week's fight was worth.
   * @returns The Tailwind text color utility to apply.
   */
  protected moraleColorClass(boss: ColonyBossView): string {
    return boss.state === 'SURVIVED' ? 'text-danger' : 'text-accent-violet';
  }

  /**
   * Colour utility for a scheduled weight class, `''` outside the run's weeks.
   *
   * Wrapped rather than called straight from the template so the `null` a node outside the ladder
   * carries resolves to no class at all instead of a lookup on `null`.
   *
   * @param category - The week's scheduled weight class, or `null`.
   * @returns The Tailwind text colour utility, or an empty string.
   */
  protected categoryColorClass(category: BossCategory | null): string {
    return category === null ? '' : resolveBossCategoryColorClass(category);
  }
}

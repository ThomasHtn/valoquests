import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  input,
  model,
  output,
  signal,
} from '@angular/core';

import {
  LucideChevronLeft,
  LucideChevronRight,
  LucideDynamicIcon,
  LucideStar,
} from '@lucide/angular';

import { BoardRow } from '@core/challenges/card/challenge-card.model';
import { CONCEPT_ICONS } from '@core/concepts/concept.constants';
import { FigurePipe } from '@core/i18n/format/figure-pipe';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Avatar } from '@shared/avatar/avatar';
import { ProgressMark } from '@shared/progress-mark/progress-mark';
import { Tooltip } from '@shared/tooltip/tooltip';

import { BoardHead } from '../board-head/board-head';
import {
  BOARD_COLUMN_REM,
  BOARD_DRAG_THRESHOLD_PX,
  BOARD_LEAD_MAX_REM,
  BOARD_LEAD_MIN_REM,
  BOARD_PAGE_SIZE,
  BOARD_ROW_STAGGER_MS,
} from '../challenges.constants';
import { BoardOperator, DayCell } from '../challenges.model';
import { DailyWeek } from '../daily-week/daily-week';

/**
 * The week as a table; operator columns slide under the challenge column when they overflow.
 */
@Component({
  selector: 'app-challenge-board',
  imports: [
    LucideDynamicIcon,
    FigurePipe,
    TranslatePipe,
    Tooltip,
    Avatar,
    BoardHead,
    DailyWeek,
    ProgressMark,
    LucideChevronLeft,
    LucideChevronRight,
    LucideStar,
  ],
  templateUrl: './challenge-board.html',
  styleUrl: './challenge-board.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[style.--column-width.px]': 'columnPx()',
    '[class.challenge-board--sliding]': 'slidable()',
    '[class.challenge-board--dragging]': 'dragging()',
    '(pointerdown)': 'pressStart($event)',
    '(pointermove)': 'pressMove($event)',
    '(pointerup)': 'pressEnd()',
    '(pointercancel)': 'pressEnd()',
    '(click)': 'swallowDragClick($event)',
    '(wheel)': 'wheel($event)',
    '(focusin)': 'reveal($event)',
  },
})
export class ChallengeBoard {
  /**
   * The squad, in board order.
   */
  public readonly operators = input.required<readonly BoardOperator[]>();

  /**
   * The challenges, the day's first.
   */
  public readonly rows = input.required<readonly BoardRow[]>();

  /**
   * The seven days of the week, for the day's tally.
   */
  public readonly days = input.required<readonly DayCell[]>();

  /**
   * The week's span over the challenge column ("Du 28 septembre au 4 octobre").
   */
  public readonly period = input.required<string>();

  /**
   * Whether validations earn wounded (running campaign) rather than points.
   */
  public readonly rescueActive = input.required<boolean>();

  /**
   * Index of the day whose challenge the daily row shows.
   */
  public readonly pickedDay = model<number | null>(null);

  /**
   * Emits the operator whose header was pressed.
   */
  public readonly pin = output<number>();

  /**
   * Icon of the footer's rewards: wounded during a campaign, points otherwise.
   */
  protected readonly rewardIcon = computed(() =>
    this.rescueActive() ? CONCEPT_ICONS.wounded : CONCEPT_ICONS.points,
  );

  /**
   * Delay between rows entering, so the table cascades in.
   */
  protected readonly rowStagger = BOARD_ROW_STAGGER_MS;

  /**
   * Host element, measured for the column widths and capturing drags.
   */
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  /**
   * Measured board width; zero until laid out or while the cards are shown.
   */
  private readonly width = signal(0);

  /**
   * Root font size in pixels, the unit of the column widths.
   */
  private readonly rem = signal(16);

  /**
   * Leftward slide of the operator columns in pixels, before clamping.
   */
  private readonly shift = signal(0);

  /**
   * Whether a press on the operator columns is dragging them.
   */
  protected readonly dragging = signal(false);

  /**
   * Tracked press: its start and the slide it started from.
   */
  private press: { pointerId: number; x: number; shift: number } | null = null;

  /**
   * Set by a drag so its closing click does not pin an operator.
   */
  private swallowClick = false;

  /**
   * Pending snap once a trackpad swipe goes quiet.
   */
  private wheelSnap: ReturnType<typeof setTimeout> | undefined;

  /**
   * Operator columns shown: a page, fewer if the squad is small or the board narrow.
   */
  protected readonly visibleCount = computed(() => {
    const count = this.operators().length;
    const width = this.width();
    if (width === 0) {
      return count;
    }
    const room = Math.floor((width - BOARD_LEAD_MIN_REM * this.rem()) / this.baseColumnPx());
    return Math.max(1, Math.min(count, BOARD_PAGE_SIZE, room));
  });

  /**
   * Challenge column's capped share, spare room going to operators; `null` until measured.
   */
  private readonly leadShare = computed(() => {
    const width = this.width();
    if (width === 0) {
      return null;
    }
    const left = width - this.visibleCount() * this.baseColumnPx();
    return Math.min(left, BOARD_LEAD_MAX_REM * this.rem());
  });

  /**
   * Operator column width (`--column-width`), in whole pixels so hairlines land on a pixel.
   */
  protected readonly columnPx = computed(() => {
    const lead = this.leadShare();
    return lead === null
      ? this.baseColumnPx()
      : Math.floor((this.width() - lead) / this.visibleCount());
  });

  /**
   * Challenge column width, absorbing rounding leftovers; `null` until measured.
   */
  protected readonly leadWidth = computed(() =>
    this.leadShare() === null ? null : this.width() - this.visibleCount() * this.columnPx(),
  );

  /**
   * Minimum operator column width in pixels, before spare room is shared out.
   */
  private readonly baseColumnPx = computed(() => BOARD_COLUMN_REM * this.rem());

  /**
   * Farthest slide, the one that shows the last operator columns.
   */
  private readonly maxShift = computed(
    () => (this.operators().length - this.visibleCount()) * this.columnPx(),
  );

  /**
   * Whether some operators are off the board and the columns can slide.
   */
  protected readonly slidable = computed(() => this.maxShift() > 0);

  /**
   * The slide drawn, clamped to the columns there are.
   */
  protected readonly offset = computed(() => Math.min(this.maxShift(), Math.max(0, this.shift())));

  /**
   * Whole table width, hidden columns included; `null` until measured.
   */
  protected readonly tableWidth = computed(() => {
    const lead = this.leadWidth();
    return lead === null ? null : lead + this.operators().length * this.columnPx();
  });

  /**
   * One-based range of operators shown, and the squad size, for the arrows' caption.
   */
  protected readonly range = computed(() => {
    const from = Math.round(this.offset() / this.columnPx()) + 1;
    return { from, to: from + this.visibleCount() - 1, total: this.operators().length };
  });

  /**
   * Measures the board once rendered and on every resize.
   */
  constructor() {
    const destroyRef = inject(DestroyRef);
    afterNextRender(() => {
      const host = this.host.nativeElement;
      const observer = new ResizeObserver(([entry]) => {
        this.rem.set(parseFloat(getComputedStyle(document.documentElement).fontSize) || 16);
        this.width.set(Math.floor(entry.contentRect.width));
      });
      observer.observe(host);
      destroyRef.onDestroy(() => {
        observer.disconnect();
        clearTimeout(this.wheelSnap);
      });
    });
  }

  /**
   * Pins an operator first and slides back to the start so they are on screen.
   */
  protected pinOperator(playerId: number): void {
    this.shift.set(0);
    this.pin.emit(playerId);
  }

  /**
   * Turns to the previous (`-1`) or next (`1`) page of operators.
   */
  protected step(direction: -1 | 1): void {
    const page = this.visibleCount() * this.columnPx();
    this.shift.set(this.snapped(this.offset()) + direction * page);
  }

  /**
   * Arms a possible drag when the operator columns are pressed.
   */
  protected pressStart(event: PointerEvent): void {
    const target = event.target as HTMLElement;
    // Only the operator columns drag; a button keeps its own press.
    if (
      !this.slidable() ||
      event.button !== 0 ||
      target.closest('button') ||
      !target.closest('.challenge-board__operator-cell')
    ) {
      return;
    }
    this.press = { pointerId: event.pointerId, x: event.clientX, shift: this.offset() };
  }

  /**
   * Slides the columns with the pointer once it has travelled past the drag threshold.
   */
  protected pressMove(event: PointerEvent): void {
    if (!this.press || event.pointerId !== this.press.pointerId) {
      return;
    }
    // Released outside the host before capture: the press is over, not a hover drag.
    if (event.buttons === 0) {
      this.pressEnd();
      return;
    }
    const travel = event.clientX - this.press.x;
    if (!this.dragging()) {
      if (Math.abs(travel) < BOARD_DRAG_THRESHOLD_PX) {
        return;
      }
      this.dragging.set(true);
      this.host.nativeElement.setPointerCapture(event.pointerId);
    }
    this.shift.set(this.press.shift - travel);
  }

  /**
   * Ends the press, snapping a drag to whole columns.
   */
  protected pressEnd(): void {
    if (this.dragging()) {
      this.shift.set(this.snapped(this.offset()));
      this.dragging.set(false);
      this.swallowClick = true;
      // The release's click fires right after; clicks work again past it.
      setTimeout(() => (this.swallowClick = false));
    }
    this.press = null;
  }

  /**
   * Cancels the click a drag release fires, so it does not pin an operator.
   */
  protected swallowDragClick(event: MouseEvent): void {
    if (this.swallowClick) {
      event.preventDefault();
      event.stopPropagation();
    }
  }

  /**
   * A sideways trackpad swipe slides the columns, then snaps to a whole column.
   */
  protected wheel(event: WheelEvent): void {
    if (!this.slidable() || Math.abs(event.deltaX) <= Math.abs(event.deltaY)) {
      return;
    }
    event.preventDefault();
    this.dragging.set(true);
    this.shift.set(this.offset() + event.deltaX);
    clearTimeout(this.wheelSnap);
    this.wheelSnap = setTimeout(() => {
      this.dragging.set(false);
      this.shift.set(this.snapped(this.offset()));
    }, 150);
  }

  /**
   * Focus on a hidden operator's star slides their column into view.
   */
  protected reveal(event: FocusEvent): void {
    const cell = (event.target as HTMLElement).closest<HTMLElement>('[data-column]');
    if (!cell || !this.slidable()) {
      return;
    }
    const column = Number(cell.dataset['column']);
    const first = Math.round(this.offset() / this.columnPx());
    if (column < first) {
      this.shift.set(column * this.columnPx());
    } else if (column >= first + this.visibleCount()) {
      this.shift.set((column - this.visibleCount() + 1) * this.columnPx());
    }
  }

  /**
   * The nearest slide that shows whole columns.
   */
  private snapped(offset: number): number {
    return Math.round(offset / this.columnPx()) * this.columnPx();
  }
}

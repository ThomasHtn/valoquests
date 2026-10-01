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
import { LucideChevronLeft, LucideChevronRight, LucideStar, LucideUsers } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Avatar } from '@shared/avatar/avatar';
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
import { BoardOperator, BoardRow, DayCell } from '../challenges.model';
import { DailyWeek } from '../daily-week/daily-week';
import { ProgressMark } from '../progress-mark/progress-mark';

/**
 * The week on one table, as the old ranking board drew it: a row per challenge, the day's first,
 * a column per operator with a dash, a ring or a check, and a footer of what each brought back.
 * The rings close row after row on arrival, the checks landing last.
 *
 * Pressing an operator's header stars them: their column moves first, the star filled in its corner.
 * When the squad outgrows the page, the challenge column stays put and the operator columns slide
 * under it: dragged, stepped with the header's arrows, or swiped sideways on a trackpad.
 */
@Component({
  selector: 'app-challenge-board',
  imports: [
    TranslatePipe,
    Tooltip,
    Avatar,
    BoardHead,
    DailyWeek,
    ProgressMark,
    LucideChevronLeft,
    LucideChevronRight,
    LucideStar,
    LucideUsers,
  ],
  templateUrl: './challenge-board.html',
  styleUrl: './challenge-board.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[style.--col.px]': 'columnPx()',
    '[class.board--sliding]': 'slidable()',
    '[class.board--dragging]': 'dragging()',
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
   * The week's span, shown over the challenge column (e.g. "Du 28 septembre au 4 octobre").
   */
  public readonly period = input.required<string>();

  /**
   * Index of the day whose challenge the daily row shows.
   */
  public readonly pickedDay = model<number | null>(null);

  /**
   * Emits the operator whose header was pressed.
   */
  public readonly pin = output<number>();

  protected readonly rowStagger = BOARD_ROW_STAGGER_MS;

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  /**
   * Width of the board, measured; zero until laid out, or while the page shows the cards instead.
   */
  private readonly width = signal(0);

  /**
   * Root font size, in pixels, which the column widths are written in.
   */
  private readonly rem = signal(16);

  /**
   * How far the operator columns slid left, in pixels, before clamping.
   */
  private readonly shift = signal(0);

  /**
   * Whether a press on the operator columns is dragging them.
   */
  protected readonly dragging = signal(false);

  /**
   * The press being tracked: where it started, and the slide it started from.
   */
  private press: { pointerId: number; x: number; shift: number } | null = null;

  /**
   * Set by a drag so the click closing it does not also pin an operator.
   */
  private swallowClick = false;

  private wheelSnap: ReturnType<typeof setTimeout> | undefined;

  /**
   * Operator columns shown at once: a page of them, fewer when the squad is smaller or the board
   * too narrow to keep the challenge column at its narrowest beside them.
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
   * Width the challenge column asks for: whatever the shown operator columns leave at their base
   * width, capped so spare room widens those columns instead; `null` before the board is measured.
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
   * Width of one operator column, in whole pixels so every hairline lands on a pixel, handed to
   * the stylesheet as `--col`: the shown columns share whatever the challenge column leaves.
   */
  protected readonly columnPx = computed(() => {
    const lead = this.leadShare();
    return lead === null
      ? this.baseColumnPx()
      : Math.floor((this.width() - lead) / this.visibleCount());
  });

  /**
   * Width of the challenge column, which takes back the pixels the rounded columns leave;
   * `null` before the board is measured.
   */
  protected readonly leadWidth = computed(() =>
    this.leadShare() === null ? null : this.width() - this.visibleCount() * this.columnPx(),
  );

  private readonly baseColumnPx = computed(() => BOARD_COLUMN_REM * this.rem());

  private readonly maxShift = computed(
    () => (this.operators().length - this.visibleCount()) * this.columnPx(),
  );

  /**
   * Whether some operators are off the board and the columns can slide.
   */
  protected readonly slidable = computed(() => this.maxShift() > 0);

  /**
   * The slide actually drawn, kept within the columns there are.
   */
  protected readonly offset = computed(() => Math.min(this.maxShift(), Math.max(0, this.shift())));

  /**
   * Width of the whole table, the hidden columns included, or `null` before the board is measured.
   */
  protected readonly tableWidth = computed(() => {
    const lead = this.leadWidth();
    return lead === null ? null : lead + this.operators().length * this.columnPx();
  });

  /**
   * First and last operator shown, counted from one, and the squad's size, for the arrows' caption.
   */
  protected readonly range = computed(() => {
    const from = Math.round(this.offset() / this.columnPx()) + 1;
    return { from, to: from + this.visibleCount() - 1, total: this.operators().length };
  });

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
   * Pins an operator first; the columns slide back to the start so they are on screen.
   */
  protected pinOperator(playerId: number): void {
    this.shift.set(0);
    this.pin.emit(playerId);
  }

  /**
   * Turns to the previous (`-1`) or next (`1`) page of operators; the last page ends on the last one.
   */
  protected step(direction: -1 | 1): void {
    const page = this.visibleCount() * this.columnPx();
    this.shift.set(this.snapped(this.offset()) + direction * page);
  }

  protected pressStart(event: PointerEvent): void {
    const target = event.target as HTMLElement;
    // Only the operator columns drag; a button keeps its own press.
    if (
      !this.slidable() ||
      event.button !== 0 ||
      target.closest('button') ||
      !target.closest('.slide')
    ) {
      return;
    }
    this.press = { pointerId: event.pointerId, x: event.clientX, shift: this.offset() };
  }

  protected pressMove(event: PointerEvent): void {
    if (!this.press || event.pointerId !== this.press.pointerId) {
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

  protected pressEnd(): void {
    if (this.dragging()) {
      this.shift.set(this.snapped(this.offset()));
      this.dragging.set(false);
      this.swallowClick = true;
      // The click, if any, fires right after the release; past it, clicks work again.
      setTimeout(() => (this.swallowClick = false));
    }
    this.press = null;
  }

  protected swallowDragClick(event: MouseEvent): void {
    if (this.swallowClick) {
      event.preventDefault();
      event.stopPropagation();
    }
  }

  /**
   * A sideways trackpad swipe slides the columns, then settles on a whole column.
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
   * Keyboard focus landing on a hidden operator's star slides their column into view.
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

import { NgTemplateOutlet } from '@angular/common';
import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { LucideChevronDown, LucideChevronLeft, LucideChevronRight } from '@lucide/angular';

import { nextInstanceId } from '@core/dom/instance-id.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Avatar } from '@shared/avatar/avatar';
import { handleListboxKeydown } from '@shared/listbox/listbox-keyboard.utils';
import { WeekOption } from '../leaderboard.model';

/**
 * Week stepper plus a listbox of every ranked week.
 * Select-only combobox like `Select`; on a phone the list is the only way to another week.
 */
@Component({
  selector: 'app-week-picker',
  imports: [
    NgTemplateOutlet,
    TranslatePipe,
    Avatar,
    LucideChevronDown,
    LucideChevronLeft,
    LucideChevronRight,
  ],
  templateUrl: './week-picker.html',
  styleUrl: './week-picker.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'relative flex items-center gap-1',
    '(document:click)': 'onDocumentClick($event)',
    '(document:keydown.escape)': 'close()',
  },
})
export class WeekPicker {
  /**
   * Every week on offer, newest first.
   */
  public readonly options = input.required<readonly WeekOption[]>();

  /**
   * Monday of the week on screen.
   */
  public readonly selected = input.required<string | null>();

  /**
   * Emits the picked Monday.
   */
  public readonly selectedChange = output<string>();

  /**
   * List id, for the trigger's `aria-controls`.
   */
  protected readonly listboxId = nextInstanceId('week-picker');

  /**
   * Whether the list is open.
   */
  protected readonly isOpen = signal(false);

  /**
   * Keyboard-highlighted index, `-1` for none; nothing is committed until confirmed.
   */
  protected readonly activeIndex = signal(-1);

  /**
   * Host, to detect outside clicks.
   */
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  /**
   * Trigger, which keeps focus while browsing.
   */
  private readonly triggerButton = viewChild.required<ElementRef<HTMLButtonElement>>('trigger');

  /**
   * List, scrolled to keep the highlight in view.
   */
  private readonly panelElement = viewChild.required<ElementRef<HTMLElement>>('panel');

  /**
   * The option on screen, shown on the trigger.
   */
  protected readonly current = computed(
    () => this.options().find((option) => option.weekStart === this.selected()) ?? null,
  );

  /**
   * Position of the week on screen in the list, `-1` when absent.
   */
  private readonly selectedIndex = computed(() =>
    this.options().findIndex((option) => option.weekStart === this.selected()),
  );

  /**
   * Whether an older week exists to step back to.
   */
  protected readonly canGoBack = computed(
    () => this.selectedIndex() >= 0 && this.selectedIndex() < this.options().length - 1,
  );

  /**
   * Whether a newer week exists to step forward to.
   */
  protected readonly canGoForward = computed(() => this.selectedIndex() > 0);

  /**
   * Highlighted option id, `null` when closed or none.
   */
  protected readonly activeOptionId = computed(() => {
    const index = this.activeIndex();
    return this.isOpen() && index >= 0 ? this.optionId(index) : null;
  });

  /**
   * Keeps the highlight in view after render: focus never leaves the trigger.
   */
  constructor() {
    afterRenderEffect(() => {
      const index = this.activeIndex();
      if (!this.isOpen() || index < 0) {
        return;
      }
      const panel: HTMLElement = this.panelElement().nativeElement;
      panel.querySelector(`#${this.optionId(index)}`)?.scrollIntoView({ block: 'nearest' });
    });
  }

  /**
   * Element id of the option at `index`.
   */
  protected optionId(index: number): string {
    return `${this.listboxId}-option-${index}`;
  }

  /**
   * Toggles the list.
   */
  protected toggle(): void {
    if (this.isOpen()) {
      this.close();
    } else {
      this.open();
    }
  }

  /**
   * Closes the list and clears the highlight.
   */
  protected close(): void {
    this.isOpen.set(false);
    this.activeIndex.set(-1);
  }

  /**
   * Steps `1` week back or `-1` forward; out of range is ignored.
   */
  protected step(offset: number): void {
    const target = this.options()[this.selectedIndex() + offset];
    if (target) {
      this.selectedChange.emit(target.weekStart);
    }
  }

  /**
   * Picks a week, closing the list and refocusing the trigger.
   */
  protected select(option: WeekOption): void {
    this.closeAndRefocus();
    this.selectedChange.emit(option.weekStart);
  }

  /**
   * Keyboard handling, bound on trigger and list so the step arrows keep native keys.
   */
  protected onKeydown(event: KeyboardEvent): void {
    handleListboxKeydown(event, {
      isOpen: this.isOpen,
      activeIndex: this.activeIndex,
      optionCount: this.options().length,
      open: () => this.open(),
      close: () => this.close(),
      closeAndRefocus: () => this.closeAndRefocus(),
      pick: (index) => {
        const option = this.options()[index];
        if (option) {
          this.select(option);
        }
      },
    });
  }

  /**
   * Whether the option starts a new run of weeks (campaign or none).
   */
  protected startsGroup(index: number): boolean {
    return index > 0 && this.options()[index - 1].group !== this.options()[index].group;
  }

  /**
   * Closes the list on an outside click.
   */
  protected onDocumentClick(event: MouseEvent): void {
    if (this.isOpen() && !this.host.nativeElement.contains(event.target as Node)) {
      this.close();
    }
  }

  /**
   * Opens the list highlighting the week on screen.
   */
  private open(): void {
    this.isOpen.set(true);
    this.activeIndex.set(Math.max(0, this.selectedIndex()));
  }

  /**
   * Closes the list and refocuses the trigger.
   */
  private closeAndRefocus(): void {
    this.close();
    this.triggerButton().nativeElement.focus();
  }
}

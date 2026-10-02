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

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Avatar } from '@shared/avatar/avatar';
import { WeekOption } from '../leaderboard.model';
import { nextInstanceId } from '@core/dom/instance-id.utils';

/**
 * The week the board shows, and the way to any other: arrows to step through them one at a time,
 * and a listbox naming every week at once — its place in its campaign, its dates, who won it.
 *
 * A listbox rather than a date picker: the board only ever shows a Monday it has a ranking for,
 * so the choice is one of a short list, never a free date.
 *
 * Follows the same select-only combobox pattern as the shared `Select`: the trigger keeps DOM focus
 * and points at the highlighted week through `aria-activedescendant`, so the list is operable with
 * arrows, Home/End, Enter, Space, Escape and Tab. That matters on a phone, where the step arrows
 * are hidden and the list is the only way to another week.
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
   * Every week the board can show, newest first.
   */
  public readonly options = input.required<readonly WeekOption[]>();

  /**
   * Monday of the week on screen.
   */
  public readonly selected = input.required<string | null>();

  /**
   * Emits the Monday of the week the reader picked.
   */
  public readonly selectedChange = output<string>();

  /**
   * Id of the list, referenced by the trigger's `aria-controls`.
   */
  protected readonly listboxId = nextInstanceId('week-picker');

  /**
   * Whether the list is open.
   */
  protected readonly isOpen = signal(false);

  /**
   * Index of the keyboard-highlighted week, or `-1` when none is; distinct from the selected one
   * so arrowing through the list commits nothing until the reader confirms.
   */
  protected readonly activeIndex = signal(-1);

  /**
   * Host element, used to detect clicks outside the open list.
   */
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  /**
   * Trigger button, which keeps focus while the list is browsed by keyboard.
   */
  private readonly triggerButton = viewChild.required<ElementRef<HTMLButtonElement>>('trigger');

  /**
   * The list, scrolled to keep the highlighted week in view.
   */
  private readonly panelElement = viewChild.required<ElementRef<HTMLElement>>('panel');

  protected readonly current = computed(
    () => this.options().find((option) => option.weekStart === this.selected()) ?? null,
  );

  private readonly selectedIndex = computed(() =>
    this.options().findIndex((option) => option.weekStart === this.selected()),
  );

  protected readonly canGoBack = computed(
    () => this.selectedIndex() >= 0 && this.selectedIndex() < this.options().length - 1,
  );

  protected readonly canGoForward = computed(() => this.selectedIndex() > 0);

  /**
   * Id of the highlighted week, or `null` when the list is closed or nothing is highlighted.
   */
  protected readonly activeOptionId = computed(() => {
    const index = this.activeIndex();
    return this.isOpen() && index >= 0 ? this.optionId(index) : null;
  });

  /**
   * Registers the effect keeping the highlighted week visible: focus never leaves the trigger, so
   * nothing else scrolls the list. After render, since the list cannot be measured while hidden.
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
   * Builds the element id of the week at `index`.
   *
   * @param index - Zero-based position in the list.
   * @returns The option's unique element id.
   */
  protected optionId(index: number): string {
    return `${this.listboxId}-option-${index}`;
  }

  /**
   * Opens or closes the list.
   */
  protected toggle(): void {
    if (this.isOpen()) {
      this.close();
    } else {
      this.open();
    }
  }

  /**
   * Closes the list and clears the keyboard highlight.
   */
  protected close(): void {
    this.isOpen.set(false);
    this.activeIndex.set(-1);
  }

  /**
   * Steps to the neighbouring week. Out-of-range steps are ignored rather than clamped, since the
   * arrows are already disabled at both ends.
   *
   * @param offset - `1` to go one week further back, `-1` to come one week forward.
   */
  protected step(offset: number): void {
    const target = this.options()[this.selectedIndex() + offset];
    if (target) {
      this.selectedChange.emit(target.weekStart);
    }
  }

  /**
   * Shows the picked week, closes the list and hands focus back to the trigger, which a tap or a
   * click on an option may have taken.
   *
   * @param option - The week picked.
   */
  protected select(option: WeekOption): void {
    this.closeAndRefocus();
    this.selectedChange.emit(option.weekStart);
  }

  /**
   * Drives the list from the keyboard, following the ARIA select-only combobox pattern. Bound on
   * the trigger and the list rather than the host, so the step arrows keep their native keys.
   *
   * @param event - The keyboard event.
   */
  protected onKeydown(event: KeyboardEvent): void {
    const lastIndex = this.options().length - 1;

    switch (event.key) {
      case 'ArrowDown':
      case 'ArrowUp': {
        event.preventDefault();
        if (!this.isOpen()) {
          this.open();
          return;
        }
        const delta = event.key === 'ArrowDown' ? 1 : -1;
        this.activeIndex.update((index) => Math.min(lastIndex, Math.max(0, index + delta)));
        return;
      }

      case 'Home':
      case 'End': {
        if (!this.isOpen()) {
          return;
        }
        event.preventDefault();
        this.activeIndex.set(event.key === 'Home' ? 0 : lastIndex);
        return;
      }

      case 'Enter':
      case ' ': {
        // Keeps the browser from also firing the trigger's native click for these keys.
        event.preventDefault();
        if (!this.isOpen()) {
          this.open();
          return;
        }
        const option = this.options()[this.activeIndex()];
        if (option) {
          this.select(option);
        }
        return;
      }

      case 'Escape': {
        if (this.isOpen()) {
          event.preventDefault();
          this.closeAndRefocus();
        }
        return;
      }

      case 'Tab': {
        // Focus leaves naturally, but never past a list left open behind it.
        this.close();
        return;
      }

      default:
        return;
    }
  }

  /**
   * Whether a rule separates this option from the one above it: the two belong to different
   * runs of weeks — one campaign, then none, then an older campaign.
   */
  protected startsGroup(index: number): boolean {
    return index > 0 && this.options()[index - 1].group !== this.options()[index].group;
  }

  /**
   * Closes the list on a click outside the control.
   *
   * @param event - The document click.
   */
  protected onDocumentClick(event: MouseEvent): void {
    if (this.isOpen() && !this.host.nativeElement.contains(event.target as Node)) {
      this.close();
    }
  }

  /**
   * Opens the list on the week on screen, so the arrows start from it.
   */
  private open(): void {
    this.isOpen.set(true);
    this.activeIndex.set(Math.max(0, this.selectedIndex()));
  }

  /**
   * Closes the list and returns focus to the trigger.
   */
  private closeAndRefocus(): void {
    this.close();
    this.triggerButton().nativeElement.focus();
  }
}

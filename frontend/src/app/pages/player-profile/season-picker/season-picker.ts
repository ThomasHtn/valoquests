import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  inject,
  input,
  model,
  signal,
  viewChild,
} from '@angular/core';
import { LucideChevronDown, LucideChevronLeft, LucideChevronRight } from '@lucide/angular';

import { nextInstanceId } from '@core/dom/instance-id.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Season } from '@core/matches/season.model';
import { SeasonPickerOption } from './season-picker.model';
import { buildSeasonPickerOptions } from './season-picker.utils';

/**
 * The profile's season scope, styled after the leaderboard's week picker: arrows stepping one act
 * at a time around a badge naming the act, and a list of every act ruled off by episode.
 *
 * Picks one season, or every season through an empty selection, by default; several at once in
 * `multiple` mode, where the list stays open while the reader ticks acts and never empties.
 *
 * Follows the same select-only combobox pattern as the shared `Select`: the trigger keeps DOM focus
 * and points at the highlighted row through `aria-activedescendant`, so the list is operable with
 * arrows, Home/End, Enter, Space, Escape and Tab. In `multiple` mode Enter and Space tick the
 * highlighted act and leave the list open.
 */
@Component({
  selector: 'app-season-picker',
  imports: [TranslatePipe, LucideChevronDown, LucideChevronLeft, LucideChevronRight],
  templateUrl: './season-picker.html',
  styleUrl: './season-picker.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'relative flex items-center gap-1',
    '(document:click)': 'onDocumentClick($event)',
    '(document:keydown.escape)': 'close()',
  },
})
export class SeasonPicker {
  /**
   * Every known season, newest first.
   */
  public readonly seasons = input.required<readonly Season[]>();

  /**
   * Selected season identifiers; empty means every season outside `multiple` mode.
   */
  public readonly selection = model<readonly number[]>([]);

  /**
   * Whether several seasons may be held at once.
   */
  public readonly multiple = input(false);

  /**
   * Largest number of seasons held at once in `multiple` mode, or zero for no limit.
   */
  public readonly maxSelection = input(0);

  /**
   * Already-translated note explaining the limit, shown once it is reached.
   */
  public readonly maxSelectionNote = input('');

  /**
   * Accessible name of the control.
   */
  public readonly ariaLabel = input.required<string>();

  /**
   * i18n service, used to spell the seasons out.
   */
  private readonly translation = inject(Translation);

  /**
   * Host element, used to detect clicks outside the open list.
   */
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  /**
   * Id of the list, referenced by the trigger's `aria-controls`.
   */
  protected readonly listboxId = nextInstanceId('season-picker');

  /**
   * Whether the list is open.
   */
  protected readonly isOpen = signal(false);

  /**
   * Index of the keyboard-highlighted row, or `-1` when none is. Counts the "every season" row
   * first in single mode (see {@link optionOffset}).
   */
  protected readonly activeIndex = signal(-1);

  /**
   * Trigger button, which keeps focus while the list is browsed by keyboard.
   */
  private readonly triggerButton = viewChild.required<ElementRef<HTMLButtonElement>>('trigger');

  /**
   * The list, scrolled to keep the highlighted row in view.
   */
  private readonly panelElement = viewChild.required<ElementRef<HTMLElement>>('panel');

  /**
   * Every season as the picker lists it, newest first.
   */
  protected readonly options = computed(() => {
    this.translation.language();
    return buildSeasonPickerOptions(this.seasons(), (key, params) =>
      this.translation.translate(key, params),
    );
  });

  /**
   * The single season held, or `null` when none or several are.
   */
  protected readonly current = computed<SeasonPickerOption | null>(() => {
    const selection = this.selection();
    return selection.length === 1
      ? (this.options().find((option) => option.id === selection[0]) ?? null)
      : null;
  });

  /**
   * Trigger label when no single season is held: every season, or how many are.
   */
  protected readonly summary = computed(() => {
    this.translation.language();
    const count = this.selection().length;
    return count === 0
      ? this.translation.translate('playerProfile.filters.allSeasons')
      : this.translation.translate('playerProfile.filters.seasonCount', { count });
  });

  /**
   * Position of the single season held in the list, or -1.
   */
  private readonly currentIndex = computed(() => {
    const current = this.current();
    return current ? this.options().indexOf(current) : -1;
  });

  /**
   * Whether an older season can be stepped to.
   */
  protected readonly canGoBack = computed(
    () => this.currentIndex() >= 0 && this.currentIndex() < this.options().length - 1,
  );

  /**
   * Whether a newer season can be stepped to.
   */
  protected readonly canGoForward = computed(() => this.currentIndex() > 0);

  /**
   * Whether the selection has reached {@link maxSelection}.
   */
  protected readonly isAtLimit = computed(() => {
    const limit = this.maxSelection();
    return this.multiple() && limit > 0 && this.selection().length >= limit;
  });

  /**
   * Rows ahead of the seasons in the list: the "every season" row of single mode, none otherwise.
   */
  protected readonly optionOffset = computed(() => (this.multiple() ? 0 : 1));

  /**
   * Id of the highlighted row, or `null` when the list is closed or nothing is highlighted.
   */
  protected readonly activeOptionId = computed(() => {
    const index = this.activeIndex();
    return this.isOpen() && index >= 0 ? this.optionId(index) : null;
  });

  /**
   * Registers the effect keeping the highlighted row visible: focus never leaves the trigger, so
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
   * Builds the element id of the row at `index`.
   *
   * @param index - Position in the list, the "every season" row included.
   * @returns The row's unique element id.
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
   * Steps the single season held to its neighbour.
   *
   * @param offset - 1 for the older season, -1 for the newer one.
   */
  protected step(offset: number): void {
    const target = this.options()[this.currentIndex() + offset];
    if (target) {
      this.selection.set([target.id]);
    }
  }

  /**
   * Picks every season, the empty selection of single mode.
   */
  protected selectAll(): void {
    this.selection.set([]);
    this.closeAndRefocus();
  }

  /**
   * Picks a season: replaces the selection in single mode, toggles it in `multiple` mode, where
   * the last season held cannot be dropped.
   *
   * @param option - The season picked.
   */
  protected select(option: SeasonPickerOption): void {
    if (!this.multiple()) {
      this.selection.set([option.id]);
      this.closeAndRefocus();
      return;
    }

    // The list stays open: the highlight follows the ticked act and focus returns to the trigger,
    // which a tap or a click on the row may have taken.
    this.activeIndex.set(this.options().indexOf(option) + this.optionOffset());
    this.triggerButton().nativeElement.focus();

    const selection = this.selection();
    if (!selection.includes(option.id)) {
      if (!this.isAtLimit()) {
        this.selection.set([...selection, option.id]);
      }
      return;
    }
    if (selection.length > 1) {
      this.selection.set(selection.filter((id) => id !== option.id));
    }
  }

  /**
   * Whether a season is held.
   *
   * @param option - The season.
   * @returns Whether it is part of the selection.
   */
  protected isSelected(option: SeasonPickerOption): boolean {
    return this.selection().includes(option.id);
  }

  /**
   * Whether the list rules a season off from the one above it: the first act of each episode.
   *
   * @param index - Position of the season in the list.
   * @returns Whether a rule goes above it.
   */
  protected startsGroup(index: number): boolean {
    const options = this.options();
    return index > 0 && options[index].era !== options[index - 1].era;
  }

  /**
   * Drives the list from the keyboard, following the ARIA select-only combobox pattern. Bound on
   * the trigger and the list rather than the host, so the step arrows keep their native keys.
   *
   * @param event - The keyboard event.
   */
  protected onKeydown(event: KeyboardEvent): void {
    const lastIndex = this.options().length + this.optionOffset() - 1;

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
        this.pickActive();
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
   * Closes the list on a click outside the control.
   *
   * @param event - The document click.
   */
  protected onDocumentClick(event: MouseEvent): void {
    if (!this.host.nativeElement.contains(event.target as Node)) {
      this.close();
    }
  }

  /**
   * Opens the list on the row held, so the arrows start from it: "every season" for an empty
   * selection in single mode, the first ticked act otherwise.
   */
  private open(): void {
    const held = this.options().findIndex((option) => this.isSelected(option));
    this.isOpen.set(true);
    this.activeIndex.set(held >= 0 ? held + this.optionOffset() : 0);
  }

  /**
   * Picks the highlighted row: every season, or one act. A season the limit greys out is left
   * alone, as its disabled row would be under the pointer.
   */
  private pickActive(): void {
    const index = this.activeIndex();
    if (this.optionOffset() === 1 && index === 0) {
      this.selectAll();
      return;
    }
    const option = this.options()[index - this.optionOffset()];
    if (option) {
      this.select(option);
    }
  }

  /**
   * Closes the list and returns focus to the trigger.
   */
  private closeAndRefocus(): void {
    this.close();
    this.triggerButton().nativeElement.focus();
  }
}

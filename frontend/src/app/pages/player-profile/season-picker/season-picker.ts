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
import { Season } from '@core/seasons/season.model';
import { handleListboxKeydown } from '@shared/listbox/listbox-keyboard.utils';
import { SeasonPickerOption } from './season-picker.model';
import { buildSeasonPickerOptions } from './season-picker.utils';

/**
 * Profile season scope styled after the week picker: one or every season, several in `multiple`.
 * Select-only combobox like `Select`: focus stays on the trigger, using `aria-activedescendant`.
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
   * Selected season ids; empty means every season outside `multiple` mode.
   */
  public readonly selection = model<readonly number[]>([]);

  /**
   * Whether several seasons may be held at once.
   */
  public readonly multiple = input(false);

  /**
   * Most seasons held at once in `multiple` mode, zero for no limit.
   */
  public readonly maxSelection = input(0);

  /**
   * Translated note shown once the limit is reached.
   */
  public readonly maxSelectionNote = input('');

  /**
   * Accessible name of the control.
   */
  public readonly ariaLabel = input.required<string>();

  /**
   * Spells the seasons out.
   */
  private readonly translation = inject(Translation);

  /**
   * Host element, to detect clicks outside the open list.
   */
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  /**
   * List id, for the trigger's `aria-controls`.
   */
  protected readonly listboxId = nextInstanceId('season-picker');

  /**
   * Whether the list is open.
   */
  protected readonly isOpen = signal(false);

  /**
   * Keyboard-highlighted row, `-1` for none; counts the "every season" row in single mode.
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
   * Seasons as the picker lists them, newest first.
   */
  protected readonly options = computed(() => {
    this.translation.language();
    return buildSeasonPickerOptions(this.seasons(), (key, params) =>
      this.translation.translate(key, params),
    );
  });

  /**
   * Single season held, `null` when none or several are.
   */
  protected readonly current = computed<SeasonPickerOption | null>(() => {
    const selection = this.selection();
    return selection.length === 1
      ? (this.options().find((option) => option.id === selection[0]) ?? null)
      : null;
  });

  /**
   * Trigger label when no single season is held: every season, or how many.
   */
  protected readonly summary = computed(() => {
    this.translation.language();
    const count = this.selection().length;
    return count === 0
      ? this.translation.translate('playerProfile.filters.allSeasons')
      : this.translation.translate('playerProfile.filters.seasonCount', { count });
  });

  /**
   * Index of the single season held, `-1` otherwise.
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
   * Whether the selection has reached `maxSelection`.
   */
  protected readonly isAtLimit = computed(() => {
    const limit = this.maxSelection();
    return this.multiple() && limit > 0 && this.selection().length >= limit;
  });

  /**
   * Rows before the seasons: the "every season" row in single mode.
   */
  protected readonly optionOffset = computed(() => (this.multiple() ? 0 : 1));

  /**
   * Id of the highlighted row, `null` when closed or none.
   */
  protected readonly activeOptionId = computed(() => {
    const index = this.activeIndex();
    return this.isOpen() && index >= 0 ? this.optionId(index) : null;
  });

  /**
   * Keeps the highlighted row visible after render, since focus never leaves the trigger.
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
   * Element id of the row at `index`, the "every season" row included.
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
   * Closes the list and clears the highlight.
   */
  protected close(): void {
    this.isOpen.set(false);
    this.activeIndex.set(-1);
  }

  /**
   * Steps the single season held: `1` to the older one, `-1` to the newer.
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
   * Replaces the selection in single mode, toggles in `multiple` where the last one stays.
   */
  protected select(option: SeasonPickerOption): void {
    if (!this.multiple()) {
      this.selection.set([option.id]);
      this.closeAndRefocus();
      return;
    }

    // The list stays open; focus returns to the trigger a tap on the row may have taken.
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
   * Whether a season is selected.
   */
  protected isSelected(option: SeasonPickerOption): boolean {
    return this.selection().includes(option.id);
  }

  /**
   * Whether a rule goes above the season: the first act of each episode.
   */
  protected startsGroup(index: number): boolean {
    const options = this.options();
    return index > 0 && options[index].era !== options[index - 1].era;
  }

  /**
   * Keyboard handler, bound on the trigger and list so the step arrows keep their native keys.
   */
  protected onKeydown(event: KeyboardEvent): void {
    handleListboxKeydown(event, {
      isOpen: this.isOpen,
      activeIndex: this.activeIndex,
      // The "every season" row of single mode is a row too.
      optionCount: this.options().length + this.optionOffset(),
      open: () => this.open(),
      close: () => this.close(),
      closeAndRefocus: () => this.closeAndRefocus(),
      pick: () => this.pickActive(),
    });
  }

  /**
   * Closes the list on a click outside the control.
   */
  protected onDocumentClick(event: MouseEvent): void {
    if (!this.host.nativeElement.contains(event.target as Node)) {
      this.close();
    }
  }

  /**
   * Opens the list on the held row ("every season" when empty), so the arrows start there.
   */
  private open(): void {
    const held = this.options().findIndex((option) => this.isSelected(option));
    this.isOpen.set(true);
    this.activeIndex.set(held >= 0 ? held + this.optionOffset() : 0);
  }

  /**
   * Picks the highlighted row; a season greyed out by the limit is left alone.
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

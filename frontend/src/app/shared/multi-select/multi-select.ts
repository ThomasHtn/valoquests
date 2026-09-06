import {
  afterRenderEffect,
  Component,
  computed,
  ElementRef,
  inject,
  input,
  model,
  signal,
  viewChild,
} from '@angular/core';
import { LucideChevronDown } from '@lucide/angular';

import { createPositionedDropdown } from '@shared/positioned-dropdown/positioned-dropdown';
import { SelectOption } from '@shared/select/select.model';

/**
 * Monotonically increasing counter backing the per-instance element ids.
 *
 * `aria-controls` and `aria-activedescendant` must resolve to exactly one element in the document,
 * so ids cannot be shared between instances.
 */
let instanceCount = 0;

/**
 * Dropdown holding several values at once, wearing the same notched surface as {@link Select}.
 *
 * A component of its own rather than a `multiple` mode on the single select: selection, the
 * trigger's label, the keyboard contract and half the ARIA attributes all differ, and branching
 * every one of them on a flag would leave the widget every filter in the application depends on
 * harder to read than the two widgets put together.
 *
 * Behaves as an ARIA multi-selectable listbox: the trigger keeps DOM focus and points at the
 * highlighted option through `aria-activedescendant`, and the panel deliberately stays open when
 * an option is toggled, since picking several values is the whole point.
 */
@Component({
  selector: 'app-multi-select',
  imports: [LucideChevronDown],
  templateUrl: './multi-select.html',
  host: {
    class: 'relative inline-block',
    '(keydown)': 'onKeydown($event)',
  },
})
export class MultiSelect<T> {
  /**
   * Options offered by the dropdown, in display order.
   */
  public readonly options = input.required<readonly SelectOption<T>[]>();

  /**
   * Accessible name for the trigger, since it otherwise only exposes a summary of the selection.
   */
  public readonly ariaLabel = input.required<string>();

  /**
   * Already-translated summary of the current selection, shown on the trigger.
   *
   * Passed in rather than assembled here: "every season", one season's name and "3 seasons" are
   * three different sentences in every language, and pluralization belongs with the dictionary.
   */
  public readonly triggerLabel = input.required<string>();

  /**
   * Largest number of values that may be held at once, or zero for no limit.
   *
   * Beyond the limit the unselected options are disabled rather than hidden, so the reader can see
   * that the list goes on and that something has to be released first.
   */
  public readonly maxSelection = input(0);

  /**
   * Already-translated note explaining the limit, shown under the options once it is reached.
   */
  public readonly maxSelectionNote = input('');

  /**
   * Currently selected values, two-way bound by the caller.
   */
  public readonly value = model<readonly T[]>([]);

  /**
   * Id of the options panel, referenced by the trigger's `aria-controls`.
   */
  protected readonly listboxId = `multi-select-listbox-${++instanceCount}`;

  /**
   * Index of the keyboard-highlighted option, or `-1` when none is highlighted.
   */
  protected readonly activeIndex = signal(-1);

  /**
   * Host element, used to detect clicks landing outside this component.
   */
  private readonly elementRef = inject(ElementRef<HTMLElement>);

  /**
   * Trigger button, refocused after the panel closes so keyboard users don't lose their place.
   */
  private readonly triggerButton = viewChild.required<ElementRef<HTMLButtonElement>>('trigger');

  /**
   * Options panel, reparented out of the host once rendered. See {@link createPositionedDropdown}.
   */
  private readonly panelElement = viewChild.required<ElementRef<HTMLDivElement>>('panel');

  /**
   * Pinning, reparenting and dismissal shared with every other positioned dropdown panel.
   */
  private readonly dropdown = createPositionedDropdown({
    host: this.elementRef,
    trigger: this.triggerButton,
    panel: this.panelElement,
  });

  /**
   * Whether the options panel is currently open.
   */
  protected readonly isOpen = this.dropdown.isOpen;

  /**
   * Viewport-relative coordinates the panel is pinned to while open.
   */
  protected readonly panelPosition = this.dropdown.panelPosition;

  /**
   * Whether the selection has reached {@link maxSelection}.
   */
  protected readonly isAtLimit = computed(() => {
    const limit = this.maxSelection();
    return limit > 0 && this.value().length >= limit;
  });

  /**
   * Id of the highlighted option, or `null` when the panel is closed or nothing is highlighted.
   */
  protected readonly activeOptionId = computed(() => {
    const index = this.activeIndex();
    return this.isOpen() && index >= 0 ? this.optionId(index) : null;
  });

  /**
   * Registers the effect keeping the highlighted option visible once the panel scrolls.
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
   * Builds the element id of the option at `index`.
   *
   * @param index - Zero-based option index.
   * @returns The option's unique element id.
   */
  protected optionId(index: number): string {
    return `${this.listboxId}-option-${index}`;
  }

  /**
   * Whether an option is currently held.
   *
   * @param option - The option to test.
   * @returns Whether its value is part of the selection.
   */
  protected isSelected(option: SelectOption<T>): boolean {
    return this.value().includes(option.value);
  }

  /**
   * Whether an option cannot currently be picked, the selection being full.
   *
   * @param option - The option to test.
   * @returns Whether picking it is refused.
   */
  protected isDisabled(option: SelectOption<T>): boolean {
    return this.isAtLimit() && !this.isSelected(option);
  }

  /**
   * Opens or closes the options panel.
   */
  protected toggle(): void {
    if (this.isOpen()) {
      this.close();
    } else {
      this.open();
    }
  }

  /**
   * Adds or removes one value, leaving the panel open.
   *
   * Releasing the last held value is refused: an empty selection would leave every chart with
   * nothing to draw, and the caller has no better answer to fall back on than the one already on
   * screen.
   *
   * @param option - The option the user toggled.
   */
  protected toggleOption(option: SelectOption<T>): void {
    if (this.isDisabled(option)) {
      return;
    }

    const selected = this.value();
    if (!selected.includes(option.value)) {
      this.value.set([...selected, option.value]);
      return;
    }
    if (selected.length > 1) {
      this.value.set(selected.filter((value) => value !== option.value));
    }
  }

  /**
   * Drives the control from the keyboard, following the ARIA listbox pattern.
   *
   * @param event - The keyboard event captured on the host.
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
        // Prevents the browser from also firing the trigger's native click for these keys.
        event.preventDefault();
        if (!this.isOpen()) {
          this.open();
          return;
        }
        const option = this.options()[this.activeIndex()];
        if (option) {
          this.toggleOption(option);
        }
        return;
      }

      case 'Escape': {
        if (this.isOpen()) {
          event.preventDefault();
          this.dropdown.closeAndRefocus();
          this.activeIndex.set(-1);
        }
        return;
      }

      case 'Tab': {
        // Let focus leave naturally, but never leave an orphaned panel open behind it.
        this.close();
        return;
      }

      default:
        return;
    }
  }

  /**
   * Opens the panel, highlighting the first held value so arrows start from the current selection.
   */
  private open(): void {
    this.dropdown.open();
    this.activeIndex.set(
      Math.max(
        0,
        this.options().findIndex((option) => this.isSelected(option)),
      ),
    );
  }

  /**
   * Closes the panel and clears the keyboard highlight.
   */
  protected close(): void {
    this.dropdown.close();
    this.activeIndex.set(-1);
  }
}

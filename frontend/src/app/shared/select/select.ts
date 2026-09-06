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
import { LucideChevronDown, LucideEllipsisVertical } from '@lucide/angular';

import { createPositionedDropdown } from '@shared/positioned-dropdown/positioned-dropdown';
import { SelectOption } from './select.model';

/**
 * Monotonically increasing counter backing the per-instance element ids.
 *
 * `aria-controls` and `aria-activedescendant` must resolve to exactly one element in the
 * document, so ids cannot be shared between instances.
 */
let instanceCount = 0;

/**
 * Custom-styled, single-select dropdown wearing the application's own direction: flat, neutral
 * surface tones, cut at the top-right corner like every other notched surface.
 *
 * Shared by every filter needing a dropdown so they all render and behave the same way. Kept as a
 * plain button-and-panel pair (no native `<select>`, no Angular Material) since the panel always
 * opens below its trigger and Material's own look would clash with this app's custom Tailwind
 * design system.
 *
 * Implements the ARIA select-only combobox pattern: the trigger keeps DOM focus and points at the
 * highlighted option through `aria-activedescendant`, so the whole control is operable with
 * arrows, Home/End, Enter, Space and Escape. The panel stays in the DOM and is hidden with the
 * `hidden` attribute when closed, so `aria-controls` always resolves to a real element.
 */
@Component({
  selector: 'app-select',
  imports: [LucideChevronDown, LucideEllipsisVertical],
  templateUrl: './select.html',
  host: {
    class: 'relative inline-block',
    '(keydown)': 'onKeydown($event)',
  },
})
export class Select<T> {
  /**
   * Options offered by the dropdown, in display order.
   */
  public readonly options = input.required<readonly SelectOption<T>[]>();

  /**
   * Accessible name for the trigger, since it otherwise only exposes the selected value.
   */
  public readonly ariaLabel = input.required<string>();

  /**
   * Currently selected value, two-way bound by the caller.
   */
  public readonly value = model<T | null>(null);

  /**
   * Whether the trigger is disabled.
   */
  public readonly disabled = input(false);

  /**
   * Already-translated message shown in the panel instead of the options list once there is
   * nothing meaningful left to choose from (zero options, or a single one that's already the
   * current value). Left empty (the default) for call sites that never hit that case and are
   * happy to keep showing whatever `options()` holds, even a lone entry.
   */
  public readonly emptyText = input('');

  /**
   * Id of the options panel, referenced by the trigger's `aria-controls`.
   */
  protected readonly listboxId = `select-listbox-${++instanceCount}`;

  /**
   * Index of the keyboard-highlighted option, or `-1` when none is highlighted.
   *
   * Distinct from the selected index: moving the highlight with the arrow keys must not commit a
   * value until the user confirms it.
   */
  protected readonly activeIndex = signal(-1);

  /**
   * Host element, used to detect clicks landing outside this component.
   */
  private readonly elementRef = inject(ElementRef<HTMLElement>);

  /**
   * Trigger button, refocused after a selection so keyboard users don't lose their place.
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
   * Index of the currently selected option, or `-1` when none matches.
   */
  protected readonly selectedIndex = computed(() =>
    this.options().findIndex((option) => option.value === this.value()),
  );

  /**
   * Label of the currently selected option, or an empty string when none matches.
   */
  protected readonly selectedLabel = computed(
    () => this.options()[this.selectedIndex()]?.label ?? '',
  );

  /**
   * Id of the highlighted option, or `null` when the panel is closed or nothing is highlighted.
   */
  protected readonly activeOptionId = computed(() => {
    const index = this.activeIndex();
    return this.isOpen() && index >= 0 ? this.optionId(index) : null;
  });

  /**
   * Registers the effect keeping the highlighted option visible once the panel scrolls.
   *
   * The panel only exposes `aria-activedescendant`, so nothing moves DOM focus and nothing scrolls
   * the list on its own; without this, arrowing past the visible options would silently highlight
   * something off-screen. Registered as an after-render effect rather than a plain one because the
   * panel cannot be measured until its `hidden` attribute has been written to the DOM.
   */
  constructor() {
    afterRenderEffect(() => {
      const index = this.activeIndex();
      if (!this.isOpen() || index < 0) {
        return;
      }

      // Queried on the panel, not on the host: the panel no longer lives under the host.
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
   * Applies the chosen option, closes the panel and returns focus to the trigger.
   *
   * @param option - The option the user picked.
   */
  protected select(option: SelectOption<T>): void {
    this.value.set(option.value);
    this.dropdown.closeAndRefocus();
    this.activeIndex.set(-1);
  }

  /**
   * Drives the control from the keyboard, following the ARIA combobox pattern.
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
          this.select(option);
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
   * Opens the panel, highlighting the selected option so arrow keys start from the current value.
   */
  private open(): void {
    this.dropdown.open();
    this.activeIndex.set(Math.max(0, this.selectedIndex()));
  }

  /**
   * Closes the panel and clears the keyboard highlight.
   */
  protected close(): void {
    this.dropdown.close();
    this.activeIndex.set(-1);
  }
}

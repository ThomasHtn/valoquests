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

import { nextInstanceId } from '@core/dom/instance-id.utils';
import { handleListboxKeydown } from '@shared/listbox/listbox-keyboard.utils';
import { createPositionedDropdown } from '@shared/positioned-dropdown/positioned-dropdown.utils';

import { SelectOption } from './select.model';

/**
 * Single-select dropdown following the ARIA select-only combobox pattern.
 * Focus stays on the trigger, which points at the highlighted option via `aria-activedescendant`.
 */
@Component({
  selector: 'app-select',
  imports: [LucideChevronDown, LucideEllipsisVertical],
  templateUrl: './select.html',
  styleUrl: './select.scss',
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
   * Accessible name of the trigger, which otherwise only shows the value.
   */
  public readonly ariaLabel = input.required<string>();

  /**
   * Selected value, two-way bound.
   */
  public readonly value = model<T | null>(null);

  /**
   * Whether the trigger is disabled.
   */
  public readonly disabled = input(false);

  /**
   * Translated message replacing a list of at most one option; empty to always list them.
   */
  public readonly emptyText = input('');

  /**
   * Id of the options panel, referenced by the trigger's `aria-controls`.
   */
  protected readonly listboxId = nextInstanceId('select-listbox');

  /**
   * Keyboard-highlighted index (`-1` for none), committed only on confirmation.
   */
  protected readonly activeIndex = signal(-1);

  /**
   * Host element, to detect outside clicks.
   */
  private readonly elementRef = inject(ElementRef<HTMLElement>);

  /**
   * Trigger, refocused after a selection so keyboard users keep their place.
   */
  private readonly triggerButton = viewChild.required<ElementRef<HTMLButtonElement>>('trigger');

  /**
   * Options panel, reparented out of the host by {@link createPositionedDropdown}.
   */
  private readonly panelElement = viewChild.required<ElementRef<HTMLDivElement>>('panel');

  /**
   * Shared pinning, reparenting and dismissal of the panel.
   */
  private readonly dropdown = createPositionedDropdown({
    host: this.elementRef,
    trigger: this.triggerButton,
    panel: this.panelElement,
  });

  /**
   * Whether the panel is open.
   */
  protected readonly isOpen = this.dropdown.isOpen;

  /**
   * Viewport coordinates of the open panel.
   */
  protected readonly panelPosition = this.dropdown.panelPosition;

  /**
   * Selected index, `-1` when none matches.
   */
  protected readonly selectedIndex = computed(() =>
    this.options().findIndex((option) => option.value === this.value()),
  );

  /**
   * Selected label, empty when none matches.
   */
  protected readonly selectedLabel = computed(
    () => this.options()[this.selectedIndex()]?.label ?? '',
  );

  /**
   * Id of the highlighted option, `null` when closed or none.
   */
  protected readonly activeOptionId = computed(() => {
    const index = this.activeIndex();
    return this.isOpen() && index >= 0 ? this.optionId(index) : null;
  });

  /**
   * Scrolls the highlight into view, after render so the panel's `hidden` is written.
   */
  constructor() {
    afterRenderEffect(() => {
      const index = this.activeIndex();
      if (!this.isOpen() || index < 0) {
        return;
      }

      // The panel no longer lives under the host.
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
   * Opens or closes the panel.
   */
  protected toggle(): void {
    if (this.isOpen()) {
      this.close();
    } else {
      this.open();
    }
  }

  /**
   * Applies the option, closes the panel and refocuses the trigger.
   */
  protected select(option: SelectOption<T>): void {
    this.value.set(option.value);
    this.dropdown.closeAndRefocus();
    this.activeIndex.set(-1);
  }

  /**
   * Keyboard handling of the ARIA combobox pattern.
   */
  protected onKeydown(event: KeyboardEvent): void {
    handleListboxKeydown(event, {
      isOpen: this.isOpen,
      activeIndex: this.activeIndex,
      optionCount: this.options().length,
      open: () => this.open(),
      close: () => this.close(),
      closeAndRefocus: () => {
        this.dropdown.closeAndRefocus();
        this.activeIndex.set(-1);
      },
      pick: (index) => {
        const option = this.options()[index];
        if (option) {
          this.select(option);
        }
      },
    });
  }

  /**
   * Opens the panel, highlighting the selected option so arrows start from it.
   */
  private open(): void {
    this.dropdown.open();
    this.activeIndex.set(Math.max(0, this.selectedIndex()));
  }

  /**
   * Closes the panel and clears the keyboard highlight.
   */
  private close(): void {
    this.dropdown.close();
    this.activeIndex.set(-1);
  }
}

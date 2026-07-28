import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  inject,
  input,
  model,
  signal,
  viewChild,
} from '@angular/core';
import { LucideChevronDown } from '@lucide/angular';

import { SelectOption } from './select.model';

/**
 * Custom-styled, single-select dropdown matching the application's pill-shaped filter design.
 *
 * Shared by every filter needing a dropdown so they all render and behave the same way. Kept as a
 * plain button-and-panel pair (no `<select>`, no Angular Material) since the panel always opens
 * below its trigger and Material's own look would clash with this app's fully custom Tailwind
 * design system.
 */
@Component({
  selector: 'app-select',
  imports: [LucideChevronDown],
  templateUrl: './select.html',
  host: {
    class: 'relative inline-block',
    '(document:click)': 'onDocumentClick($event)',
    '(keydown.escape)': 'close()',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Select<T> {
  /**
   * Options offered by the dropdown, in display order.
   */
  public readonly options = input.required<readonly SelectOption<T>[]>();

  /**
   * Accessible name for the trigger button, since it otherwise only exposes the selected value.
   */
  public readonly ariaLabel = input.required<string>();

  /**
   * Currently selected value, two-way bound by the caller.
   */
  public readonly value = model<T | null>(null);

  /**
   * Whether the options panel is currently open.
   */
  protected readonly isOpen = signal(false);

  /**
   * Host element, used to detect clicks landing outside this component.
   */
  private readonly elementRef = inject(ElementRef<HTMLElement>);

  /**
   * Trigger button, refocused after a selection so keyboard users don't lose their place.
   */
  private readonly triggerButton = viewChild.required<ElementRef<HTMLButtonElement>>('trigger');

  /**
   * Label of the currently selected option, or an empty string when none matches.
   */
  protected readonly selectedLabel = computed(
    () => this.options().find((option) => option.value === this.value())?.label ?? '',
  );

  /**
   * Opens or closes the options panel.
   */
  protected toggle(): void {
    this.isOpen.update((open) => !open);
  }

  /**
   * Closes the options panel.
   */
  protected close(): void {
    this.isOpen.set(false);
  }

  /**
   * Applies the chosen option, closes the panel and returns focus to the trigger.
   *
   * @param option - The option the user picked.
   */
  protected select(option: SelectOption<T>): void {
    this.value.set(option.value);
    this.close();
    this.triggerButton().nativeElement.focus();
  }

  /**
   * Closes the panel when a click lands outside this component.
   *
   * @param event - The document-wide click event.
   */
  protected onDocumentClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target as Node)) {
      this.close();
    }
  }
}

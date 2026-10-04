import { Signal, WritableSignal } from '@angular/core';

/**
 * State and actions of a listbox driven by {@link handleListboxKeydown}.
 */
export interface ListboxKeyboardControls {
  /**
   * Whether the list is open.
   */
  readonly isOpen: Signal<boolean>;

  /**
   * Highlighted row, `-1` for none.
   */
  readonly activeIndex: WritableSignal<number>;

  /**
   * Number of rows.
   */
  readonly optionCount: number;

  /**
   * Opens the list and highlights its starting row.
   */
  open(): void;

  /**
   * Closes the list without moving focus, as Tab already moves it elsewhere.
   */
  close(): void;

  /**
   * Closes the list and returns focus to the trigger, as Escape does.
   */
  closeAndRefocus(): void;

  /**
   * Confirms the row at `index` (Enter, Space); it may match no option in an empty list.
   */
  pick(index: number): void;
}

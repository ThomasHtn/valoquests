import { ElementRef, Signal } from '@angular/core';

/**
 * Viewport coordinates of an open dropdown panel.
 */
export interface DropdownPanelPosition {
  /**
   * Px from the viewport top, `null` when opening upwards.
   */
  top: number | null;

  /**
   * Px from the viewport bottom, set only when opening upwards.
   */
  bottom: number | null;

  /**
   * Px from the viewport right edge.
   */
  right: number;

  /**
   * Minimum panel width in px.
   */
  minWidth: number;

  /**
   * List height cap in px, shrunk to the room on the opening side so no option ends off-screen.
   */
  maxHeight: number;
}

/**
 * Refs a positioned dropdown needs to pin, reparent and dismiss its panel.
 */
export interface PositionedDropdownRefs {
  /**
   * Host element, to detect clicks outside the whole control.
   */
  host: ElementRef<HTMLElement>;

  /**
   * Trigger button, refocused by {@link PositionedDropdown.closeAndRefocus}.
   */
  trigger: Signal<ElementRef<HTMLButtonElement>>;

  /**
   * Options panel, reparented out of the host once rendered.
   */
  panel: Signal<ElementRef<HTMLElement>>;
}

/**
 * Open state, position and controls of a positioned dropdown.
 */
export interface PositionedDropdown {
  /**
   * Whether the panel is open.
   */
  readonly isOpen: Signal<boolean>;

  /**
   * Where the panel is pinned while open.
   */
  readonly panelPosition: Signal<DropdownPanelPosition>;

  /**
   * Opens the panel under the trigger, or above it when only that side has room.
   */
  open(): void;

  /**
   * Closes the panel, leaving focus where it is.
   */
  close(): void;

  /**
   * Closes and refocuses the trigger, for a caller-driven dismissal (Escape, selection).
   */
  closeAndRefocus(): void;
}

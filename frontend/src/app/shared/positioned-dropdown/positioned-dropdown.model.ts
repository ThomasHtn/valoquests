import { ElementRef, Signal } from '@angular/core';

/**
 * Viewport-relative coordinates a dropdown panel is pinned to while open.
 */
export interface DropdownPanelPosition {
  /**
   * Distance from the viewport top, in pixels.
   */
  top: number;

  /**
   * Distance from the viewport right edge, in pixels.
   */
  right: number;

  /**
   * Minimum panel width, in pixels.
   */
  minWidth: number;
}

/**
 * Refs a positioned dropdown needs to pin, reparent and dismiss its panel.
 */
export interface PositionedDropdownRefs {
  /**
   * Host element of the trigger, used to detect a click landing outside the whole control.
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
  open(): void;
  close(): void;
  toggle(): void;

  /**
   * Closes the panel and returns focus to the trigger, for a caller-driven dismissal (Escape, a
   * confirmed selection) as opposed to one detected from the outside (a click, a resize, a scroll).
   */
  closeAndRefocus(): void;
}

import { afterNextRender, DestroyRef, ElementRef, inject, Signal, signal } from '@angular/core';

/**
 * Viewport-relative coordinates a dropdown panel is pinned to while open.
 */
export interface DropdownPanelPosition {
  top: number;
  right: number;
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
  readonly isOpen: Signal<boolean>;
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

/**
 * Wires the behaviour shared by every fixed-position, portalled dropdown panel in the design
 * system: pinning the panel under its trigger, reparenting it past any clip-path/overflow
 * ancestor, and closing it on an outside click, a window resize or a scroll — every one of which
 * invalidates the pinned coordinates.
 *
 * `position: fixed` alone only escapes an ancestor's `overflow: hidden` — it does not escape a
 * `clip-path` (e.g. a `notch-tr` card), which clips its whole painted subtree regardless of how a
 * descendant is positioned. Moving the panel node itself out of the host sidesteps that. It moves
 * to the nearest enclosing modal `<dialog>` when there is one, and to `document.body` otherwise: a
 * modal dialog paints in the top layer and renders the rest of the document inert, so a panel
 * parked on the body would sit behind the backdrop and take no clicks.
 *
 * Deliberately silent on what the caller highlights or selects: this module only owns whether the
 * panel is open and where it sits, so a combobox's own keyboard navigation and selection state stay
 * with the caller.
 *
 * Must be called from a component's constructor or a field initializer, since it relies on the
 * injection context to register its listeners' cleanup.
 *
 * @param refs - The trigger, panel and host refs to pin, reparent and watch.
 * @returns The dropdown's open state, position and controls.
 */
export function createPositionedDropdown(refs: PositionedDropdownRefs): PositionedDropdown {
  const { host, trigger, panel } = refs;
  const destroyRef = inject(DestroyRef);

  const isOpen = signal(false);
  const panelPosition = signal<DropdownPanelPosition>({ top: 0, right: 0, minWidth: 0 });

  afterNextRender(() => {
    const panelHost = host.nativeElement.closest('dialog') ?? document.body;
    panelHost.appendChild(panel().nativeElement);
  });
  destroyRef.onDestroy(() => panel().nativeElement.remove());

  const onDocumentClick = (event: MouseEvent): void => {
    if (!isOpen()) {
      return;
    }

    const target = event.target as Node;
    if (!host.nativeElement.contains(target) && !panel().nativeElement.contains(target)) {
      close();
    }
  };
  document.addEventListener('click', onDocumentClick);
  destroyRef.onDestroy(() => document.removeEventListener('click', onDocumentClick));

  window.addEventListener('resize', close);
  destroyRef.onDestroy(() => window.removeEventListener('resize', close));

  // `scroll` doesn't bubble, so a window-level listener would miss scrolling that happens inside a
  // container (e.g. the app's own scrollable `<main>`) rather than the window itself. Listening on
  // the capture phase still sees it, wherever it happens, since capture fires while the event
  // travels down toward its target. Closing rather than repositioning keeps this simple and avoids
  // the panel trailing a stale position for a frame.
  const onScroll = (): void => {
    if (isOpen()) {
      close();
    }
  };
  window.addEventListener('scroll', onScroll, true);
  destroyRef.onDestroy(() => window.removeEventListener('scroll', onScroll, true));

  function open(): void {
    const rect = trigger().nativeElement.getBoundingClientRect();
    panelPosition.set({
      top: rect.bottom + 8,
      right: window.innerWidth - rect.right,
      minWidth: rect.width,
    });
    isOpen.set(true);
  }

  function close(): void {
    isOpen.set(false);
  }

  function toggle(): void {
    if (isOpen()) {
      close();
    } else {
      open();
    }
  }

  function closeAndRefocus(): void {
    close();
    trigger().nativeElement.focus();
  }

  return { isOpen, panelPosition, open, close, toggle, closeAndRefocus };
}

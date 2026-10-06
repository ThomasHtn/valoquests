import { afterNextRender, DestroyRef, inject, signal } from '@angular/core';

import {
  DROPDOWN_GAP_PX,
  DROPDOWN_LIST_MAX_PX,
  DROPDOWN_ROOM_PX,
} from './positioned-dropdown.constants';
import {
  DropdownPanelPosition,
  PositionedDropdown,
  PositionedDropdownRefs,
} from './positioned-dropdown.model';

/**
 * Pins a fixed dropdown panel under its trigger, closing it on outside click, resize or scroll.
 * Needs an injection context. The panel moves out of the host since `fixed` cannot escape a
 * `clip-path`, into the enclosing modal `<dialog>` if any, which would otherwise make it inert.
 */
export function createPositionedDropdown(refs: PositionedDropdownRefs): PositionedDropdown {
  const { host, trigger, panel } = refs;
  const destroyRef = inject(DestroyRef);

  const isOpen = signal(false);
  const panelPosition = signal<DropdownPanelPosition>({
    top: 0,
    bottom: null,
    right: 0,
    minWidth: 0,
    maxHeight: DROPDOWN_LIST_MAX_PX,
  });

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

  // Capture phase: `scroll` does not bubble, and inner containers like `<main>` scroll too.
  const onScroll = (event: Event): void => {
    // Scrolling the panel's own list keeps its position valid.
    if (isOpen() && !panel().nativeElement.contains(event.target as Node)) {
      close();
    }
  };
  window.addEventListener('scroll', onScroll, true);
  destroyRef.onDestroy(() => window.removeEventListener('scroll', onScroll, true));

  function open(): void {
    const rect = trigger().nativeElement.getBoundingClientRect();
    const below = window.innerHeight - rect.bottom;
    // Upwards when the list does not fit below but has more room above.
    const upwards = below < DROPDOWN_ROOM_PX && rect.top > below;
    const room = (upwards ? rect.top : below) - 2 * DROPDOWN_GAP_PX;
    panelPosition.set({
      top: upwards ? null : rect.bottom + DROPDOWN_GAP_PX,
      bottom: upwards ? window.innerHeight - rect.top + DROPDOWN_GAP_PX : null,
      right: window.innerWidth - rect.right,
      minWidth: rect.width,
      maxHeight: Math.max(0, Math.min(DROPDOWN_LIST_MAX_PX, room)),
    });
    isOpen.set(true);
  }

  function close(): void {
    isOpen.set(false);
  }

  function closeAndRefocus(): void {
    close();
    trigger().nativeElement.focus();
  }

  return { isOpen, panelPosition, open, close, closeAndRefocus };
}

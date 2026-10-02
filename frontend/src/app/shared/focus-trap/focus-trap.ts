import { Directive, ElementRef, inject, input } from '@angular/core';

import { focusableWithin } from './focus-trap.utils';

/**
 * Keeps Tab and Shift+Tab inside a modal layer while it is active, as `aria-modal` promises: from
 * the last control focus wraps to the first, and back. A layer with nothing focusable keeps focus
 * on itself.
 */
@Directive({
  selector: '[appFocusTrap]',
  host: { '(keydown)': 'onKeydown($event)' },
})
export class FocusTrap {
  /**
   * Whether the trap holds, for a layer that stays in the DOM while closed (the mobile drawer).
   */
  public readonly appFocusTrap = input(true, {
    transform: (value: boolean | '') => value !== false,
  });

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key !== 'Tab' || !this.appFocusTrap()) {
      return;
    }
    const root = this.host.nativeElement;
    const items = focusableWithin(root);
    if (items.length === 0) {
      event.preventDefault();
      root.focus();
      return;
    }
    const first = items[0];
    const last = items[items.length - 1];
    const active = root.ownerDocument.activeElement;
    if (event.shiftKey && (active === first || active === root)) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && active === last) {
      event.preventDefault();
      first.focus();
    }
  }
}

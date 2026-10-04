import { Directive, ElementRef, inject, input } from '@angular/core';

import { focusableWithin } from './focus-trap.utils';

/**
 * Wraps Tab inside a modal layer, as `aria-modal` promises; an empty layer keeps focus itself.
 */
@Directive({
  selector: '[appFocusTrap]',
  host: { '(keydown)': 'onKeydown($event)' },
})
export class FocusTrap {
  /**
   * Whether the trap holds, for layers kept in the DOM while closed.
   */
  public readonly appFocusTrap = input(true, {
    transform: (value: boolean | '') => value !== false,
  });

  /**
   * Layer element, whose focusables the trap cycles through.
   */
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  /**
   * Wraps Tab and Shift+Tab around the first and last focusables.
   */
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

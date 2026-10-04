import { FOCUSABLE_SELECTOR } from './focus-trap.constants';

/**
 * Visible focusable descendants of `root`, in tab order.
 */
export function focusableWithin(root: HTMLElement): HTMLElement[] {
  return Array.from(root.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR)).filter(
    (element) => !element.closest('[inert]') && element.getClientRects().length > 0,
  );
}

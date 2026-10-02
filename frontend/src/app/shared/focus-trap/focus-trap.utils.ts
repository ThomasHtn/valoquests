import { FOCUSABLE_SELECTOR } from './focus-trap.constants';

/**
 * The focusable elements of a layer, in tab order, leaving out those hidden from view.
 *
 * @param root - The trapped layer.
 * @returns Its focusable descendants.
 */
export function focusableWithin(root: HTMLElement): HTMLElement[] {
  return Array.from(root.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR)).filter(
    (element) => !element.closest('[inert]') && element.getClientRects().length > 0,
  );
}

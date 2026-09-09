/**
 * Unique element ids for components rendered several times on one page (listboxes, tooltips,
 * SVG gradients), so `aria-controls` and `url(#...)` references never collide.
 */

/**
 * Counter shared by every prefix; ids only need to be unique, not dense.
 */
let nextInstance = 0;

/**
 * Returns a new id of the form `<prefix>-<n>`.
 */
export function nextInstanceId(prefix: string): string {
  nextInstance += 1;
  return `${prefix}-${nextInstance}`;
}

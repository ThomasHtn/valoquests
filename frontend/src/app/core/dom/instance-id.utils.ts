/**
 * Counter shared by every prefix: ids need to be unique, not dense.
 */
let nextInstance = 0;

/**
 * New `<prefix>-<n>` id, so `aria-controls` and `url(#...)` never collide across instances.
 */
export function nextInstanceId(prefix: string): string {
  nextInstance += 1;
  return `${prefix}-${nextInstance}`;
}

/**
 * Narrows a wrapped block to its longest line, which CSS alone cannot do.
 */
export function fitToLines(element: HTMLElement): void {
  element.style.width = '';
  const range = document.createRange();
  range.selectNodeContents(element);
  const lines = [...range.getClientRects()];
  const wrapped = new Set(lines.map((line) => Math.round(line.top))).size > 1;
  if (!wrapped) {
    return;
  }
  const left = element.getBoundingClientRect().left;
  const widest = Math.max(...lines.map((line) => line.right - left));
  element.style.width = `${Math.ceil(widest)}px`;
}

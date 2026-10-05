import { BLUEPRINT_INK_CHANNELS } from './rocket-showcase.constants';

/**
 * Blueprint ink at `opacity` percent, for SVG attributes that cannot read CSS tokens.
 */
export function blueprintInk(opacity = 100): string {
  return `rgb(${BLUEPRINT_INK_CHANNELS} / ${opacity}%)`;
}

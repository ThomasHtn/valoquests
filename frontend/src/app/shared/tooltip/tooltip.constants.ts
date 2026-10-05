import { TooltipIconNode } from './tooltip.model';

/**
 * Gap in px between the host and its bubble, and between the bubble and the viewport edges.
 */
export const TOOLTIP_OFFSET = 8;

/**
 * Lucide `user` glyph, built by hand since the bubble lives outside any template.
 */
export const TOOLTIP_FALLBACK_ICON: readonly TooltipIconNode[] = [
  { tag: 'path', attributes: { d: 'M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2' } },
  { tag: 'circle', attributes: { cx: '12', cy: '7', r: '4' } },
];

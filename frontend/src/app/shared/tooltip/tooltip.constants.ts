import { TooltipIconNode } from './tooltip.model';

/**
 * Hover surface class, shared with the battle map card; no shadow, `clip-path` would cut it.
 */
export const TOOLTIP_SURFACE_CLASS =
  'notch-tr border-l-2 border-brand-500/50 bg-surface-sunken text-text-primary [--notch:0.375rem]';

/**
 * Gap in px between the host and its bubble.
 */
export const OFFSET = 8;

/**
 * Portrait beside the text, the same disc as `app-avatar`.
 */
export const TOOLTIP_PORTRAIT_CLASS = 'size-9 shrink-0 rounded-full object-cover';

/**
 * Missing-portrait disc, matching `app-avatar`'s fallback.
 */
export const TOOLTIP_PORTRAIT_FALLBACK_CLASS =
  'flex size-9 shrink-0 items-center justify-center rounded-full bg-surface-700 text-text-secondary';

/**
 * Lucide `user` glyph, built by hand since the bubble lives outside any template.
 */
export const TOOLTIP_FALLBACK_ICON: readonly TooltipIconNode[] = [
  { tag: 'path', attributes: { d: 'M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2' } },
  { tag: 'circle', attributes: { cx: '12', cy: '7', r: '4' } },
];

import { TooltipIconNode } from './tooltip.model';

/**
 * Silhouette shared by every floating surface that describes the thing under the pointer.
 *
 * The direction's treatment for panels that hover above the page, matching the select listbox and
 * the profile page's game-mode menu: cut corner, held by a leading brand rule rather than a full
 * border. No shadow — `clip-path` clips one along with the corner it cuts, and the rule is what
 * lifts the surface off what is behind it.
 *
 * Declared here rather than inline in {@link Tooltip} because the battle map's hover card has to
 * match it exactly and cannot use the directive itself: the directive renders text, and that card
 * carries player avatars.
 */
export const TOOLTIP_SURFACE_CLASS =
  'notch-tr border-l-2 border-brand-500/50 bg-surface-sunken text-text-primary [--notch:0.375rem]';

/**
 * Distance in pixels between the host element and its tooltip.
 */
export const OFFSET = 8;

/**
 * Delay a tooltip hung off a whole block waits before opening, in milliseconds.
 *
 * Long enough that crossing the block on the way somewhere else never opens the bubble, short
 * enough that stopping on it to ask "what is this" does not feel like waiting. Shared so the two
 * overview blocks answer at the same pace rather than each picking a number.
 */
export const BLOCK_TOOLTIP_DELAY_MS = 400;

/**
 * Portrait beside a tooltip's text, cut to the same disc as `app-avatar` everywhere else.
 */
export const TOOLTIP_PORTRAIT_CLASS = 'size-9 shrink-0 rounded-full object-cover';

/**
 * Disc standing in for a missing portrait, matching `app-avatar`'s own fallback.
 */
export const TOOLTIP_PORTRAIT_FALLBACK_CLASS =
  'flex size-9 shrink-0 items-center justify-center rounded-full bg-surface-700 text-text-secondary';

/**
 * Lucide `user` glyph drawn inside {@link TOOLTIP_PORTRAIT_FALLBACK_CLASS}, built by hand because
 * the bubble is created outside any component template.
 */
export const TOOLTIP_FALLBACK_ICON: readonly TooltipIconNode[] = [
  { tag: 'path', attributes: { d: 'M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2' } },
  { tag: 'circle', attributes: { cx: '12', cy: '7', r: '4' } },
];

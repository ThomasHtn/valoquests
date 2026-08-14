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

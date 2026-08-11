/**
 * Supported avatar sizes, from the compact rows of a table to a profile page's hero portrait.
 */
export type AvatarSize = 'xs' | 'sm' | 'md' | 'lg';

/**
 * Supported avatar silhouettes.
 *
 * `hex` is the framing the direction gives a player who is *placed* — on the podium, in the
 * contributor stack — echoing the hexagon position badge. It has to be resolved here rather than
 * clipped by the call site: the portrait's own rounding would otherwise survive inside the
 * hexagon and eat its corners.
 */
export type AvatarShape = 'circle' | 'hex';

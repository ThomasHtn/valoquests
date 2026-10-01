/**
 * Canvas width under which the tooltip sits above or below the mark rather than beside it: on a
 * phone a side bubble would cover half the plot or run off the screen.
 */
export const CHART_TOOLTIP_COMPACT_WIDTH = 520;

/**
 * Gap between the mark and the bubble, in pixels.
 */
export const CHART_TOOLTIP_GAP = 16;

/**
 * Half the bubble's widest size, so a centred bubble never crosses the canvas' side edges.
 */
export const CHART_TOOLTIP_HALF_WIDTH = 135;

/**
 * Room kept free beside a side bubble before it flips to the other side of the mark.
 */
export const CHART_TOOLTIP_SIDE_ROOM = 300;

/**
 * Half the usual bubble's height, so a side bubble stays inside the canvas vertically.
 */
export const CHART_TOOLTIP_HALF_HEIGHT = 90;

/**
 * How long a figure takes to reach its value, in milliseconds.
 *
 * Long enough to be read as a climb rather than a flicker, short enough that nobody waits on it. The
 * figure is legible throughout — this is not a loading state.
 */
export const DURATION_MS = 1100;

/**
 * Share of a figure that must be on screen before its first climb starts, so a number below the
 * fold does not finish counting before anyone scrolls to it.
 */
export const VISIBILITY_THRESHOLD = 0.35;

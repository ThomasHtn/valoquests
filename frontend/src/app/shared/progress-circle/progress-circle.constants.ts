/**
 * Ring geometry, in the SVG viewBox's own units. The viewBox scales to whatever size the caller
 * gave the host, so the stroke stays a constant share of the diameter — 4 in 44, the share
 * `progress-ring-core` clears when it draws a disc inside the ring.
 */
export const RING_SIZE = 44;

/**
 * Stroke width of the ring, in viewBox units.
 */
export const RING_STROKE = 4;

/**
 * Radius of the ring, keeping the stroke inside the viewBox.
 */
export const RING_RADIUS = (RING_SIZE - RING_STROKE) / 2;

/**
 * Length of the ring, the dash array of the arc.
 */
export const RING_CIRCUMFERENCE = 2 * Math.PI * RING_RADIUS;

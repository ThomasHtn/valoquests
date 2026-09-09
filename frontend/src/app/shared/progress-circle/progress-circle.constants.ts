/**
 * Ring geometry, in the SVG viewBox's own units. The viewBox scales to whatever size the caller
 * gave the host, so the stroke stays a constant share of the diameter — 4 in 44, the share
 * `progress-ring-core` clears when it draws a disc inside the ring.
 */
export const RING_SIZE = 44;
export const RING_STROKE = 4;
export const RING_RADIUS = (RING_SIZE - RING_STROKE) / 2;
export const RING_CIRCUMFERENCE = 2 * Math.PI * RING_RADIUS;

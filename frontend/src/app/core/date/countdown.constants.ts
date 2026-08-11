/**
 * Interval at which a "time remaining" countdown is refreshed.
 *
 * Lives in `core` rather than beside one page: both the overview and the challenges page render
 * the same countdown from their own ticking clock, and neither should own the other's cadence.
 */
export const COUNTDOWN_REFRESH_INTERVAL_MS = 60_000;

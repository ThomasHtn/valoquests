/**
 * Outcome of one tracked player's match.
 *
 * Mirrors the backend `MatchResult` enum. `UNKNOWN` is used when Henrik does not expose a reliable
 * team result for the mode.
 */
export type MatchResult = 'WIN' | 'LOSS' | 'DRAW' | 'REMAKE' | 'UNKNOWN';

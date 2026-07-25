package io.github.thomashtn.valorant.tracker.match.model;

/**
 * Defines the supported result values for a tracked player's match.
 */
public enum MatchResult {
    WIN,
    LOSS,
    DRAW,
    REMAKE,

    /**
     * Used when Henrik does not expose a reliable team result for the mode.
     */
    UNKNOWN
}

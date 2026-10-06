package io.github.thomashtn.valoquests.match.model;

/**
 * Result of a tracked player's match as Henrik reported it, stored and filtered on as is.
 *
 * <p>Scoring reads {@link ScoredOutcome} instead, which reinterprets Deathmatch and unknown results.
 */
public enum MatchResult {
    WIN,
    LOSS,

    /**
     * Henrik reports a drawn match as lost by both teams, on equal rounds.
     */
    DRAW,

    /**
     * Used when Henrik does not expose a reliable team result for the mode.
     */
    UNKNOWN
}

package io.github.thomashtn.valoquests.match.model;

/**
 * Normalized outcome of one valued match, independent of how it was derived.
 *
 * <p>For team-based modes this mirrors {@link io.github.thomashtn.valoquests.match.model.MatchResult}.
 * For {@link io.github.thomashtn.valoquests.match.model.GameMode#DEATHMATCH}, which has no reliable
 * team result, it is derived instead from the player reaching 40 kills.
 */
public enum MatchOutcome {
    WIN,
    LOSS,
    DRAW
}

package io.github.thomashtn.valoquests.match.model;

/**
 * Result of one match as scored: what damage and challenge progress count, never unknown.
 *
 * <p>Resolved by {@link io.github.thomashtn.valoquests.match.service.MatchOutcomeResolver} from the
 * stored {@link MatchResult}: an unknown result scores as a loss, and
 * {@link GameMode#DEATHMATCH}, which has no reliable team result, scores a win at 40 kills.
 */
public enum ScoredOutcome {
    WIN,
    LOSS,
    DRAW
}

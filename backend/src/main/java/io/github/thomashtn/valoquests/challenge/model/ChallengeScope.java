package io.github.thomashtn.valoquests.challenge.model;

/**
 * Defines the level at which a challenge condition must be evaluated.
 *
 * <p>Aggregating over the period is what every calculator does when no scope is given, so that
 * default has no constant of its own.
 */
public enum ChallengeScope {

    /**
     * Evaluates the condition independently for every eligible match.
     *
     * <p>This scope is suitable for occurrence and streak challenges where
     * each match must satisfy a specific threshold.</p>
     */
    PER_MATCH
}

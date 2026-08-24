package io.github.thomashtn.valoquests.challenge.model;

/**
 * Defines the level at which a challenge condition must be evaluated.
 *
 * <p>A weekly scope constant used to sit alongside {@link #PER_MATCH} and was never once declared by a
 * catalogue rule: aggregating over the week is what every calculator does when no scope is given, so
 * naming it only offered a second way to say the default.
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

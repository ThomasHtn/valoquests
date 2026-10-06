package io.github.thomashtn.valoquests.challenge.model;

/**
 * Defines the level at which a challenge condition must be evaluated.
 *
 * <p>Aggregating over the period is the default when no scope is given, so it has no constant.
 */
public enum ChallengeScope {

    /**
     * Evaluates the condition independently for every eligible match, as occurrences and streaks need.
     */
    PER_MATCH
}

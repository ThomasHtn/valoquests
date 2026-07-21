package io.github.thomashtn.valorant.tracker.challenge.model;

/**
 * Defines the level at which a challenge condition must be evaluated.
 */
public enum ChallengeScope {

    /**
     * Evaluates the condition across all eligible matches of the week.
     *
     * <p>This scope is suitable for aggregated metrics such as total kills,
     * total assists, total damage or a weekly kill-to-death ratio.</p>
     */
    WEEKLY,

    /**
     * Evaluates the condition independently for every eligible match.
     *
     * <p>This scope is suitable for occurrence and streak challenges where
     * each match must satisfy a specific threshold.</p>
     */
    PER_MATCH
}

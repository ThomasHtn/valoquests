package io.github.thomashtn.valoquests.challenge.model;

/**
 * Defines the supported progress mode values.
 */
public enum ProgressMode {
    /**
     * Adds the metric up over the window.
     */
    SUM,

    /**
     * Counts the distinct values of a grouping key, such as agents played.
     */
    DISTINCT_COUNT,

    /**
     * Keeps the best total reached by one group, such as one map.
     */
    MAX_GROUP,

    /**
     * Requires every condition, each counted up to its own target.
     */
    ALL,

    /**
     * Counts the matches that satisfy the condition.
     */
    COUNT_MATCHES,

    /**
     * Computes a rate from totals over every eligible match, not an average of per-match rates.
     */
    RATIO,

    /**
     * Keeps the longest run of consecutive matches satisfying the condition.
     */
    MAX_STREAK
}

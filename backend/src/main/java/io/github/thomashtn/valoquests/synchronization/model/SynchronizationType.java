package io.github.thomashtn.valoquests.synchronization.model;

/**
 * Defines the supported synchronization type values.
 *
 * <p>Only an execution that calls the Henrik API is recorded here. Challenge, ranking and campaign
 * recalculations read exclusively from PostgreSQL and have never been persisted as executions.
 */
public enum SynchronizationType {

    /**
     * The half-hourly walk of the current act and the one before it.
     */
    STANDARD,

    /**
     * The one-off walk of a whole calibration window, run before a campaign is opened.
     */
    HISTORY_BACKFILL
}

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
     * Legacy value, no longer written: kept because older execution rows still hold it.
     */
    HISTORY_BACKFILL
}

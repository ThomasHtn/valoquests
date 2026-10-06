package io.github.thomashtn.valoquests.synchronization.model;

/**
 * Defines the supported synchronization type values.
 *
 * <p>Only executions calling the Henrik API are recorded; recalculations never are.
 */
public enum SynchronizationType {

    /**
     * The five-minute walk of the current act and the one before it.
     */
    STANDARD,

    /**
     * Legacy value, no longer written: kept because older execution rows still hold it.
     */
    HISTORY_BACKFILL
}

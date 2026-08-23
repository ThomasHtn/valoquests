package io.github.thomashtn.valoquests.synchronization.model;

/**
 * Defines the supported synchronization type values.
 *
 * <p>Only an execution that calls the Henrik API is recorded as a synchronization, which is why a
 * single value remains. Challenge and ranking recalculations read exclusively from PostgreSQL and
 * have never been persisted here, so no stored row can carry another value.
 */
public enum SynchronizationType {
    STANDARD
}

package io.github.thomashtn.valoquests.synchronization.model;

/**
 * Defines the supported synchronization trigger values.
 */
public enum SynchronizationTrigger {
    /**
     * Started by the five-minute scheduler.
     */
    SCHEDULED,

    /**
     * Started by an administrator request.
     */
    MANUAL
}

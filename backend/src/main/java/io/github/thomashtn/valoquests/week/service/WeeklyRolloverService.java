package io.github.thomashtn.valoquests.week.service;

/**
 * Finalizes the previous week and prepares the current one.
 */
public interface WeeklyRolloverService {

    /**
     * Performs the weekly rollover when required.
     *
     * <p>The operation is idempotent and can safely be called several times.</p>
     */
    void rolloverIfNeeded();
}

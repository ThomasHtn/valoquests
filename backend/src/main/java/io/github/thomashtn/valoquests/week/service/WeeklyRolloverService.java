package io.github.thomashtn.valoquests.week.service;

/**
 * Finalizes the previous week and prepares the current one.
 */
public interface WeeklyRolloverService {

    /**
     * Performs the weekly rollover when required.
     *
     * <p>Idempotent: safe to call several times.
     */
    void rolloverIfNeeded();
}

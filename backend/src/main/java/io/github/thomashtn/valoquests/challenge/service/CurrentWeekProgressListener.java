package io.github.thomashtn.valoquests.challenge.service;

/**
 * Told once the current week's challenge progress has been rebuilt, inside the same transaction.
 *
 * <p>Implemented by the ranking package, which the challenges cannot call directly.
 */
public interface CurrentWeekProgressListener {

    /**
     * Reacts to the current week's challenge progress having just been rebuilt.
     */
    void currentWeekProgressRecalculated();
}

package io.github.thomashtn.valoquests.challenge.service;

/**
 * Told once the current week's challenge progress has been rebuilt, inside the same transaction.
 *
 * <p>The ranking package implements it to rebuild the current standings: the ranking sits above the
 * challenges, which cannot call it directly.
 */
public interface CurrentWeekProgressListener {

    /**
     * Reacts to the current week's challenge progress having just been rebuilt.
     */
    void currentWeekProgressRecalculated();
}

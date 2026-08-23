package io.github.thomashtn.valoquests.challenge.service;

import java.time.LocalDate;

/**
 * Defines the challenge-progress recalculation operations.
 *
 * <p>Progress is always rebuilt from the matches already stored in PostgreSQL. Neither operation
 * calls the Henrik API, so importing the missing matches is the caller's responsibility.
 */
public interface ChallengeRecalculationService {

    /**
     * Recalculates active-week progress and then updates the current ranking.
     */
    void recalculateCurrentWeekProgress();

    /**
     * Recalculates the progress of one week without touching any ranking.
     *
     * <p>Exists for the weekly rollover, which must refresh the closing week from the matches
     * imported since the last synchronization before freezing it, and which rebuilds that week's
     * ranking itself as part of the same transaction.
     *
     * <p>A week holding no challenge pack is left untouched: unlike the current week, a past week
     * must never have a pack created retroactively.
     *
     * @param weekStart Monday identifying the week to rebuild
     */
    void recalculateWeekProgress(LocalDate weekStart);
}

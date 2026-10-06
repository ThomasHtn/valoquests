package io.github.thomashtn.valoquests.challenge.service;

import java.time.LocalDate;

/**
 * Defines the challenge-progress recalculation operations.
 *
 * <p>Rebuilt from stored matches only; importing from Henrik is the caller's job.
 */
public interface ChallengeRecalculationService {

    /**
     * Draws the current week's pack and today's challenge when missing, recalculates the week's
     * progress, then updates the current ranking.
     */
    void drawAndRecalculateCurrentWeek();

    /**
     * Recalculates the progress of one week without touching any ranking.
     *
     * <p>Used by the weekly rollover, which rebuilds the ranking itself. A week without a pack is left
     * untouched: a past week never gets a pack retroactively.
     *
     * @param weekStart Monday identifying the week to rebuild
     */
    void recalculateWeekProgress(LocalDate weekStart);
}

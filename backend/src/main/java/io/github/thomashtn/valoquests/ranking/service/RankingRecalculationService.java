package io.github.thomashtn.valoquests.ranking.service;

import java.time.LocalDate;

/**
 * Defines ranking recalculation operations.
 */
public interface RankingRecalculationService {

    /**
     * Recalculates active-week scores, positions and position variations.
     */
    void recalculateCurrentRanking();

    /**
     * Recalculates scores, positions and position variations for one week.
     *
     * <p>Mainly used when finalizing a week; reads stored progress only, never Henrik.
     *
     * @param weekStart Monday identifying the week to recalculate
     */
    void recalculateWeek(LocalDate weekStart);
}

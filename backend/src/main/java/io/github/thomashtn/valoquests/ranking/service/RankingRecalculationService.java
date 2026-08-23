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
     * <p>This operation is primarily used when finalizing the previous week.
     * It only uses challenge progress already stored in PostgreSQL and does
     * not contact the Henrik API.</p>
     *
     * @param weekStart Monday identifying the week to recalculate
     */
    void recalculateWeek(LocalDate weekStart);
}

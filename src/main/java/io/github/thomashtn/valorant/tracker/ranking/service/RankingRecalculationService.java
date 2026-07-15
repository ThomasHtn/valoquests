package io.github.thomashtn.valorant.tracker.ranking.service;

/** Defines the manual ranking-only recalculation operation. */
public interface RankingRecalculationService {

    /** Recalculates active-week scores, positions and position variations. */
    void recalculateCurrentRanking();
}

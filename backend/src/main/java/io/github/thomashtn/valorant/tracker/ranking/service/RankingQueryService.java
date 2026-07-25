package io.github.thomashtn.valorant.tracker.ranking.service;

import io.github.thomashtn.valorant.tracker.ranking.dto.CurrentRankingResponse;
import io.github.thomashtn.valorant.tracker.ranking.dto.RankingHistoryWeekResponse;
import io.github.thomashtn.valorant.tracker.shared.dto.PageResponse;

/**
 * Defines read operations for current and historical rankings.
 */
public interface RankingQueryService {

    /**
     * Returns the ranking calculated for the active week.
     *
     * @return current weekly ranking
     */
    CurrentRankingResponse findCurrent();

    /**
     * Returns finalized weekly rankings using page-based pagination.
     *
     * @param page zero-based page index
     * @param size requested page size
     * @return a page of finalized weekly rankings
     */
    PageResponse<RankingHistoryWeekResponse> findHistory(int page, int size);
}

package io.github.thomashtn.valoquests.ranking.service;

import io.github.thomashtn.valoquests.ranking.dto.CurrentRankingResponse;
import io.github.thomashtn.valoquests.ranking.dto.DailyRankingResponse;
import io.github.thomashtn.valoquests.ranking.dto.RankingHistoryWeekResponse;
import io.github.thomashtn.valoquests.shared.dto.PageResponse;
import java.time.LocalDate;

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

    /**
     * Returns one day's ranking, read back off the stored matches.
     *
     * <p>Nothing is persisted at this scale: unlike the weekly board, a day is priced on demand from
     * the matches it holds, through the same barème the week and the colony read.
     *
     * @param day day to rank, or {@code null} for today
     * @return that day's ranking, and how it compares to the day before
     */
    DailyRankingResponse findDaily(LocalDate day);
}

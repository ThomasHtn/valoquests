package io.github.thomashtn.valoquests.boss.service;

import io.github.thomashtn.valoquests.boss.dto.BossHistoryWeekResponse;
import io.github.thomashtn.valoquests.boss.dto.CurrentBossResponse;
import io.github.thomashtn.valoquests.shared.dto.PageResponse;

/**
 * Provides read-only access to the current and historical boss confrontations.
 */
public interface BossQueryService {

    /**
     * Returns the active week's boss confrontation.
     *
     * @return current boss confrontation
     */
    CurrentBossResponse findCurrent();

    /**
     * Returns finalized weekly boss confrontations using week-based pagination.
     *
     * @param page zero-based page index
     * @param size number of finalized weeks returned per page
     * @return one page of finalized confrontations, most recent week first
     */
    PageResponse<BossHistoryWeekResponse> findHistory(int page, int size);
}

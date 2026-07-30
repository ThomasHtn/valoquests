package io.github.thomashtn.valorant.tracker.match.service;

import io.github.thomashtn.valorant.tracker.match.dto.MatchResponse;
import io.github.thomashtn.valorant.tracker.shared.dto.PageResponse;

/**
 * Defines read operations for player match history.
 */
public interface MatchQueryService {

    /**
     * Returns a filtered and paginated match history for one player.
     *
     * @param playerId internal player identifier
     * @param page zero-based page index
     * @param size requested page size
     * @param seasonId optional season identifier
     * @param map optional map filter
     * @param agent optional agent filter
     * @param result optional match result filter
     * @param gameMode optional game mode filter
     * @return a page containing matching player matches
     */
    PageResponse<MatchResponse> findByPlayer(
        long playerId,
        int page,
        int size,
        Long seasonId,
        String map,
        String agent,
        String result,
        String gameMode
    );
}

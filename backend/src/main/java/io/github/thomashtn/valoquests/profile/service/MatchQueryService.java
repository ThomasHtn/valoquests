package io.github.thomashtn.valoquests.profile.service;

import io.github.thomashtn.valoquests.match.dto.MatchHistoryFilter;
import io.github.thomashtn.valoquests.profile.dto.MatchDetailResponse;
import io.github.thomashtn.valoquests.profile.dto.MatchResponse;
import io.github.thomashtn.valoquests.profile.dto.SquadMatchResponse;
import io.github.thomashtn.valoquests.shared.dto.PageResponse;

/**
 * Defines read operations for player match history.
 */
public interface MatchQueryService {

    /**
     * Returns a filtered and paginated match history for one player.
     *
     * @param playerId internal player identifier
     * @param page     zero-based page index
     * @param size     requested page size
     * @param filter   optional season, map, agent, result and game mode filters
     * @return a page containing matching player matches
     */
    PageResponse<MatchResponse> findByPlayer(
        long playerId,
        int page,
        int size,
        MatchHistoryFilter filter
    );

    /**
     * Returns a page of the shown campaign roster's matches of the day, newest first, empty without one.
     *
     * @param page zero-based page index
     * @param size requested page size
     * @return a page containing the squad's matches
     */
    PageResponse<SquadMatchResponse> findSquad(int page, int size);

    /**
     * Returns full detail for one of a player's matches, with the other tracked players in it.
     *
     * @param playerId      internal player identifier
     * @param playerMatchId internal player-match identifier
     * @return the requested match's full detail
     */
    MatchDetailResponse findDetail(long playerId, long playerMatchId);
}

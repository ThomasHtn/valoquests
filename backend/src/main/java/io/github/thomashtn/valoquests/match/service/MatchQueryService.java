package io.github.thomashtn.valoquests.match.service;

import io.github.thomashtn.valoquests.match.dto.MatchDetailResponse;
import io.github.thomashtn.valoquests.match.dto.MatchResponse;
import io.github.thomashtn.valoquests.match.dto.SquadMatchResponse;
import io.github.thomashtn.valoquests.match.model.MatchHistoryFilter;
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
     * Returns a page of the squad's matches of the day: those of the shown campaign's roster, newest
     * first, each named after its player. Empty without a campaign.
     *
     * @param page zero-based page index
     * @param size requested page size
     * @return a page containing the squad's matches
     */
    PageResponse<SquadMatchResponse> findSquad(int page, int size);

    /**
     * Returns full detail for one of a tracked player's matches, including every other tracked
     * player found in the same match.
     *
     * @param playerId      internal player identifier
     * @param playerMatchId internal player-match identifier
     * @return the requested match's full detail
     */
    MatchDetailResponse findDetail(long playerId, long playerMatchId);
}

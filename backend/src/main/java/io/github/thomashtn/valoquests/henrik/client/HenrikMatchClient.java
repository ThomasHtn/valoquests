package io.github.thomashtn.valoquests.henrik.client;

import io.github.thomashtn.valoquests.henrik.dto.match.HenrikMatchHistoryResponse;

/**
 * Defines Henrik API operations related to Valorant match history.
 */
public interface HenrikMatchClient {

    /**
     * Largest page one request may ask for: the application keeps pages small to bound each call.
     *
     * <p>A page shorter than this is the end of the player's history.
     */
    int MAX_PAGE_SIZE = 10;

    /**
     * Retrieves one page of recent matches for a Riot player.
     *
     * @param puuid Riot's unique player identifier
     * @param start zero-based pagination start index
     * @param size maximum number of matches to retrieve
     * @return Henrik match-history response
     */
    HenrikMatchHistoryResponse getMatches(
        String puuid,
        int start,
        int size
    );
}

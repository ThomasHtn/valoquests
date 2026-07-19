package io.github.thomashtn.valorant.tracker.henrik.client;

import io.github.thomashtn.valorant.tracker.henrik.dto.mmr.HenrikMmrResponse;

/** Defines Henrik API operations related to a player's current MMR. */
public interface HenrikMmrClient {

    /**
     * Retrieves the current competitive rank of a Riot player.
     *
     * @param puuid Riot's unique player identifier
     * @return current Henrik MMR response
     */
    HenrikMmrResponse getCurrentMmr(String puuid);
}

package io.github.thomashtn.valorant.tracker.player.service;

import io.github.thomashtn.valorant.tracker.player.dto.PlayerDetailsResponse;
import io.github.thomashtn.valorant.tracker.player.dto.PlayerSummaryResponse;
import java.util.List;

/**
 * Defines read operations for tracked players.
 */
public interface PlayerQueryService {

    /**
     * Returns all tracked players with their summary statistics.
     *
     * @return tracked player summaries
     */
    List<PlayerSummaryResponse> findAll();

    /**
     * Returns the detailed profile of one tracked player.
     *
     * @param playerId internal player identifier
     * @return detailed player data
     */
    PlayerDetailsResponse findById(long playerId);
}

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
     * @param seasonId optional season identifier restricting the statistics; {@code null} for every season
     * @param gameMode optional game mode restricting the statistics; {@code null} for every mode
     * @return detailed player data
     */
    PlayerDetailsResponse findById(long playerId, Long seasonId, String gameMode);
}

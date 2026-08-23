package io.github.thomashtn.valoquests.player.service;

import io.github.thomashtn.valoquests.player.dto.PlayerDetailsResponse;
import io.github.thomashtn.valoquests.player.dto.PlayerSummaryResponse;
import java.time.LocalDate;
import java.util.List;

/**
 * Defines read operations for tracked players.
 */
public interface PlayerQueryService {

    /**
     * Returns all tracked players with their summary statistics, scoped to the season currently in
     * progress and to competitive matches.
     *
     * @return tracked player summaries
     */
    List<PlayerSummaryResponse> findAll();

    /**
     * Returns the detailed profile of one tracked player.
     *
     * @param playerId  internal player identifier
     * @param seasonId  optional season identifier restricting the statistics; {@code null} for every season
     * @param gameMode  optional game mode restricting the statistics; {@code null} for every mode
     * @param weekStart optional Monday restricting the statistics to that calendar week; {@code null}
     *     for every week
     * @return detailed player data
     */
    PlayerDetailsResponse findById(long playerId, Long seasonId, String gameMode, LocalDate weekStart);
}

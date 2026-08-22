package io.github.thomashtn.valorant.tracker.player.service;

import io.github.thomashtn.valorant.tracker.player.dto.PlayerProgressionResponse;
import java.util.List;

/**
 * Defines the analytics read by the player profile's progression view.
 */
public interface PlayerProgressionQueryService {

    /**
     * Returns one player's progression analytics.
     *
     * @param playerId  internal player identifier
     * @param seasonIds seasons to restrict the analytics to; empty or {@code null} for every season
     * @return the player's progression analytics
     */
    PlayerProgressionResponse findByPlayerId(long playerId, List<Long> seasonIds);
}

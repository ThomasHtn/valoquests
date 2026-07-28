package io.github.thomashtn.valorant.tracker.match.service;

import io.github.thomashtn.valorant.tracker.match.dto.SeasonResponse;
import java.util.List;

/**
 * Defines read operations for seasons available to filter match history.
 */
public interface SeasonQueryService {

    /**
     * Returns every known season, most recent first.
     *
     * @return known seasons
     */
    List<SeasonResponse> findAll();
}

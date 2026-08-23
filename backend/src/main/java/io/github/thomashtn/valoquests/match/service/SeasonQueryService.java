package io.github.thomashtn.valoquests.match.service;

import io.github.thomashtn.valoquests.match.dto.SeasonResponse;
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

    /**
     * Resolves the season currently in progress - the most recent one known.
     *
     * @return the current season's identifier, or {@code null} if no season is known yet
     */
    Long resolveCurrentSeasonId();
}

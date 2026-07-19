package io.github.thomashtn.valorant.tracker.henrik.dto.match;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Represents the root response returned by the Henrik match-history endpoint.
 *
 * <p>The Henrik API wraps the retrieved matches inside a {@code data} array.
 * Unknown properties are ignored so that additional fields introduced by
 * Henrik do not break deserialization.</p>
 *
 * @param status HTTP-like status returned in the Henrik response body
 * @param data matches returned for the requested player
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HenrikMatchHistoryResponse(
    Integer status,
    List<HenrikMatchData> data
) {

    /**
     * Prevents the match collection from being null or mutable.
     */
    public HenrikMatchHistoryResponse {
        data = data == null ? List.of() : List.copyOf(data);
    }

    /**
     * Represents one match returned by Henrik.
     *
     * @param metadata general information about the match
     * @param players players who participated in the match
     * @param teams teams and final result of the match
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikMatchData(
        HenrikMatchMetadata metadata,
        List<HenrikMatchPlayer> players,
        List<HenrikMatchTeam> teams
    ) {

        /**
         * Prevents nested collections from being null or mutable.
         */
        public HenrikMatchData {
            players = players == null ? List.of() : List.copyOf(players);
            teams = teams == null ? List.of() : List.copyOf(teams);
        }
    }
}

package io.github.thomashtn.valorant.tracker.henrik.dto.match;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/** Contains the general metadata of a Valorant match returned by Henrik. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HenrikMatchMetadata(
    @JsonProperty("match_id") String matchId,
    HenrikMap map,
    @JsonProperty("game_length_in_ms") Long gameLengthInMilliseconds,
    @JsonProperty("started_at") Instant startedAt,
    @JsonProperty("is_completed") Boolean completed,
    HenrikQueue queue,
    HenrikSeason season
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikMap(String id, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikQueue(
        String id,
        String name,
        @JsonProperty("mode_type") String modeType
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikSeason(
        String id,
        @JsonProperty("short") String shortName
    ) {}
}

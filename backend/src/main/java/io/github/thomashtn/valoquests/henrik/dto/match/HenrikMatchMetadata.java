package io.github.thomashtn.valoquests.henrik.dto.match;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Contains the general metadata of a Valorant match returned by Henrik.
 */
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
    /**
     * Identifies the map a match was played on.
     *
     * @param id   Henrik map identifier
     * @param name human-readable map name
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikMap(String id, String name) {}

    /**
     * Identifies the queue a match was played in.
     *
     * <p>Henrik fills these fields inconsistently, so the game mode is resolved from all of them.
     *
     * @param id       Henrik queue identifier, such as {@code competitive}
     * @param name     human-readable queue name
     * @param modeType queue category reported by Henrik
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikQueue(

        String id,
        String name,
        @JsonProperty("mode_type") String modeType
    ) {}

    /**
     * Identifies the act a match belongs to.
     *
     * <p>The identifier bounds a synchronization walk, so a match without one is rejected.
     *
     * @param id        Henrik act identifier, such as {@code e11a4}
     * @param shortName abbreviated act name, such as {@code V26A4}
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikSeason(

        String id,
        @JsonProperty("short") String shortName
    ) {}
}

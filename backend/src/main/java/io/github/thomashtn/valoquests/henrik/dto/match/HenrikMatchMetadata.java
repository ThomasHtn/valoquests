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
     * <p>Henrik populates {@code id} and {@code modeType} inconsistently across game modes, which
     * is why {@code HenrikMatchMapper} resolves the game mode from several of these fields rather
     * than trusting any single one.
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
     * <p>The identifier is what bounds a synchronization walk: the import stops when it leaves the
     * current act, so a match whose season identifier is missing cannot be placed and is rejected.
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

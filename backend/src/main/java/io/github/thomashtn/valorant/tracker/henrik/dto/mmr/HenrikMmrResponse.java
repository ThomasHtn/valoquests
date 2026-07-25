package io.github.thomashtn.valorant.tracker.henrik.dto.mmr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Root response returned by Henrik's Valorant MMR v3 endpoint.
 *
 * @param status HTTP-like status embedded in the Henrik response
 * @param data current competitive information for the requested player
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HenrikMmrResponse(

    Integer status,
    HenrikMmrData data
) {

    /**
     * Contains the current competitive state.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikMmrData(

        HenrikCurrentMmr current
    ) {}

    /**
     * Current rank information returned by Henrik.
     *
     * @param tier current Valorant competitive tier
     * @param rankRating current Rank Rating inside the tier
     * @param elo global competitive ELO exposed by Henrik
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikCurrentMmr(

        HenrikTier tier,
        @JsonProperty("rr") Integer rankRating,
        Integer elo
    ) {}

    /**
     * Valorant competitive tier metadata.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikTier(

        Integer id,
        String name
    ) {}
}

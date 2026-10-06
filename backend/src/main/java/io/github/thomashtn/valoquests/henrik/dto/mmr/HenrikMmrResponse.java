package io.github.thomashtn.valoquests.henrik.dto.mmr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Root response returned by Henrik's Valorant MMR v3 endpoint.
 *
 * @param data current competitive information for the requested player
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HenrikMmrResponse(

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
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikCurrentMmr(

        HenrikTier tier,
        @JsonProperty("rr") Integer rankRating
    ) {}

    /**
     * Valorant competitive tier metadata.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikTier(

        String name
    ) {}
}

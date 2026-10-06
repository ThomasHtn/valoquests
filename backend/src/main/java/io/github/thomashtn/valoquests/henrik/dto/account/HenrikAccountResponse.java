package io.github.thomashtn.valoquests.henrik.dto.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the response returned by the Henrik account endpoint.
 *
 * @param data resolved Riot account information
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HenrikAccountResponse(

    HenrikAccountData data
) {

    /**
     * Represents the useful Riot account fields returned by Henrik.
     *
     * @param puuid stable Riot account identifier
     * @param gameName current Riot game name
     * @param tagLine current Riot tag line
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikAccountData(

        String puuid,
        @JsonProperty("name") String gameName,
        @JsonProperty("tag") String tagLine
    ) {
    }
}

package io.github.thomashtn.valoquests.henrik.dto.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the response returned by the Henrik account endpoint.
 *
 * <p>Only fields required by the application are declared. Unknown properties
 * are deliberately ignored so that additional Henrik fields do not break
 * deserialization.</p>
 *
 * @param status Henrik response status
 * @param data resolved Riot account information
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HenrikAccountResponse(

    Integer status,
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

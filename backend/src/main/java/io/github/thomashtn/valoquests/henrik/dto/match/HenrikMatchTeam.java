package io.github.thomashtn.valoquests.henrik.dto.match;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a team participating in a Valorant match.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HenrikMatchTeam(

    @JsonProperty("team_id") String teamId,
    Boolean won,
    HenrikRounds rounds
) {
    /**
     * Reports how many rounds a team won and lost.
     *
     * @param won  rounds won by the team
     * @param lost rounds lost by the team
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikRounds(Integer won, Integer lost) {}

}

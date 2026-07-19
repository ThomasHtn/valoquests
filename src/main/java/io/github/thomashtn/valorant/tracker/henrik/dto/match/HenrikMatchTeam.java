package io.github.thomashtn.valorant.tracker.henrik.dto.match;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Represents a team participating in a Valorant match. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HenrikMatchTeam(
    @JsonProperty("team_id") String teamId,
    Boolean won,
    HenrikRounds rounds
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikRounds(Integer won, Integer lost) {}
}

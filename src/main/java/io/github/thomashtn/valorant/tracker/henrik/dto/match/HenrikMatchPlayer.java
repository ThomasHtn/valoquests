package io.github.thomashtn.valorant.tracker.henrik.dto.match;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a player who participated in a Valorant match.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HenrikMatchPlayer(
    String puuid,
    String name,
    String tag,
    @JsonProperty("team_id") String teamId,
    HenrikAgent agent,
    HenrikPlayerStats stats,
    HenrikTier tier
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikAgent(String id, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikPlayerStats(
        Integer score,
        Integer kills,
        Integer deaths,
        Integer assists,
        Integer headshots,
        Integer bodyshots,
        Integer legshots,
        HenrikDamage damage
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikDamage(Integer dealt, Integer received) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikTier(Integer id, String name) {}
}

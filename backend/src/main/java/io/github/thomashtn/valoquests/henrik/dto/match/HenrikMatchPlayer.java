package io.github.thomashtn.valoquests.henrik.dto.match;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a player who participated in a Valorant match.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HenrikMatchPlayer(

    String puuid,
    @JsonProperty("team_id") String teamId,
    HenrikAgent agent,
    HenrikPlayerStats stats,
    HenrikTier tier
) {
    /**
     * Identifies the agent a player used.
     *
     * @param id   Henrik agent identifier
     * @param name human-readable agent name
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikAgent(String id, String name) {}

    /**
     * Carries one player's scoreboard for a single match.
     *
     * <p>Counters are boxed: Henrik omits them in some modes, and {@code null} means "not reported".
     *
     * @param score     combat score
     * @param kills     kills scored
     * @param deaths    times the player died
     * @param assists   assists credited
     * @param headshots shots that hit the head
     * @param bodyshots shots that hit the body
     * @param legshots  shots that hit the legs
     * @param damage    damage dealt
     */
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

    /**
     * Reports the damage one player dealt.
     *
     * @param dealt damage dealt to opponents
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikDamage(Integer dealt) {}

    /**
     * Identifies the competitive tier a player held during the match.
     *
     * @param name human-readable tier name, such as {@code Gold 2}
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikTier(String name) {}

}

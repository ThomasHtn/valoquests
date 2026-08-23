package io.github.thomashtn.valoquests.henrik.dto.match;

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
     * <p>Every counter is boxed because Henrik omits them for some game modes rather than sending
     * zero. A {@code null} therefore means "not reported", which the mapper must not confuse with
     * a genuine zero.
     *
     * @param score     combat score
     * @param kills     kills scored
     * @param deaths    times the player died
     * @param assists   assists credited
     * @param headshots shots that hit the head
     * @param bodyshots shots that hit the body
     * @param legshots  shots that hit the legs
     * @param damage    damage dealt and received
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
     * Reports the damage exchanged by one player.
     *
     * @param dealt    damage dealt to opponents
     * @param received damage taken from opponents
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikDamage(Integer dealt, Integer received) {}

    /**
     * Identifies the competitive tier a player held during the match.
     *
     * @param id   Henrik tier identifier
     * @param name human-readable tier name, such as {@code Gold 2}
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HenrikTier(Integer id, String name) {}

}

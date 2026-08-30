package io.github.thomashtn.valoquests.match.dto;

import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Exposes another tracked player's line in a match both of them played.
 *
 * <p>The squad is small enough that two tracked players routinely queue into the same lobby; a
 * match's detail surfaces every one of them found on either side, rather than only the requesting
 * player's own statistics.
 *
 * @param playerId    internal identifier of the other tracked player
 * @param displayName the other player's display name
 * @param portrait    agent name backing the other player's bundled avatar, {@code null} when unset
 * @param agentName   agent the other player picked in this match
 * @param sameTeam    whether the other player shared the requesting player's team
 * @param result      the other player's own result for the match
 * @param kills       kills scored by the other player
 * @param deaths      times the other player died
 * @param assists     assists credited to the other player
 * @param acs         the other player's average combat score for the match
 */
@Schema(description = "Another tracked player's line in a match both of them played.")
public record MatchTeammateResponse(

    Long playerId,
    String displayName,
    String portrait,
    String agentName,
    boolean sameTeam,
    MatchResult result,
    int kills,
    int deaths,
    int assists,
    BigDecimal acs
) {
}

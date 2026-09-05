package io.github.thomashtn.valoquests.campaign.dto;

import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchResult;

/**
 * The match that brought a week's guardian down, as the mission report names it.
 *
 * @param mapName    map the match was played on
 * @param gameMode   mode of the match
 * @param result     the operator's result
 * @param allyScore  rounds won by the operator's team, {@code null} when the mode keeps no score
 * @param enemyScore rounds won by the other team, {@code null} when the mode keeps no score
 * @param agentName  agent the operator played
 */
public record FatalBlowResponse(
    String mapName,
    GameMode gameMode,
    MatchResult result,
    Integer allyScore,
    Integer enemyScore,
    String agentName
) {
}

package io.github.thomashtn.valoquests.challenge.calculator;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import org.springframework.stereotype.Component;

/**
 * Applies the common match filters declared by challenge conditions.
 */
@Component
public class ChallengeMatchFilter {

    /**
     * Determines whether a player match belongs to the condition scope.
     *
     * @param playerMatch persisted player-match data
     * @param condition   parsed challenge condition
     * @return {@code true} when the match must be evaluated
     */
    public boolean matches(
        PlayerMatch playerMatch,
        ChallengeCondition condition
    ) {
        return condition
            .effectiveGameMode()
            .matches(playerMatch.getMatch().getGameMode());
    }
}

package io.github.thomashtn.valoquests.challenge.calculator;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.service.MatchEligibility;
import org.springframework.stereotype.Component;

/**
 * Applies the common match filters declared by challenge conditions.
 */
@Component
public class ChallengeMatchFilter {

    /**
     * Shared rule deciding whether a match counts at all.
     */
    private final MatchEligibility matchEligibility;

    /**
     * Creates the challenge match filter.
     *
     * @param matchEligibility shared match eligibility rule
     */
    public ChallengeMatchFilter(MatchEligibility matchEligibility) {
        this.matchEligibility = matchEligibility;
    }

    /**
     * Determines whether a player match belongs to the condition scope.
     *
     * <p>Eligibility is checked here, not in each calculator, so a remake never progresses a challenge
     * and a play day means the same as for the regularity bonus.
     *
     * @param playerMatch persisted player-match data
     * @param condition   parsed challenge condition
     * @return {@code true} when the match must be evaluated
     */
    public boolean matches(
        PlayerMatch playerMatch,
        ChallengeCondition condition
    ) {
        return matchEligibility.isEligible(playerMatch)
            && condition
            .effectiveGameMode()
            .matches(playerMatch.getMatch().getGameMode());
    }
}

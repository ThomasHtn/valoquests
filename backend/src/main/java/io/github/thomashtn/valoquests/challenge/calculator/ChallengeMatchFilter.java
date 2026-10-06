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
     * <p>Eligibility is checked before the game mode, and deliberately not left to the individual
     * calculators, so a remake worth no damage never progresses a volume target and "play on four
     * different days" agrees with the regularity bonus on what a day is.
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

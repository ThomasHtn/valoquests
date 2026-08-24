package io.github.thomashtn.valoquests.scoring.service;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.service.MatchEligibility;
import io.github.thomashtn.valoquests.match.service.MatchOutcomeResolver;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import org.springframework.stereotype.Component;

/**
 * Resolves whether one played match is valued and, when it is, how much damage it deals.
 */
@Component
public final class MatchDamageCalculator {

    /**
     * Shared rule deciding whether a match counts at all.
     */
    private final MatchEligibility matchEligibility;

    /**
     * Shared rule deciding how a match ended.
     */
    private final MatchOutcomeResolver outcomeResolver;

    /**
     * Creates the match damage calculator.
     *
     * @param matchEligibility shared match eligibility rule
     * @param outcomeResolver  shared match outcome rule
     */
    public MatchDamageCalculator(
        MatchEligibility matchEligibility,
        MatchOutcomeResolver outcomeResolver
    ) {
        this.matchEligibility = matchEligibility;
        this.outcomeResolver = outcomeResolver;
    }

    /**
     * Determines whether a played match is valued at all.
     *
     * @param playerMatch tracked player's statistics for the match
     * @return {@code true} when the match was really played and was not remade
     */
    public boolean isEligible(PlayerMatch playerMatch) {
        return matchEligibility.isEligible(playerMatch);
    }

    /**
     * Resolves the damage dealt by one match, or zero when it is not eligible.
     *
     * @param playerMatch tracked player's statistics for the match
     * @param ruleset     ruleset the owning week or boss encounter was resolved against
     * @return damage inflicted by this match
     */
    public int damageOf(PlayerMatch playerMatch, ScoringRuleset ruleset) {
        if (!isEligible(playerMatch)) {
            return 0;
        }

        return ruleset.matchDamage(
            playerMatch.getMatch().getGameMode(),
            outcomeResolver.outcomeOf(playerMatch)
        );
    }
}

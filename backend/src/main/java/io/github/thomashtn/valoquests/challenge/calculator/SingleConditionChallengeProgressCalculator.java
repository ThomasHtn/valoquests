package io.github.thomashtn.valoquests.challenge.calculator;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import java.math.BigDecimal;
import java.util.List;

/**
 * Base of the calculators whose challenge holds one condition, measured over the matches it accepts.
 *
 * <p>Subclasses only measure; filtering and the comparison with the target happen here.
 */
public abstract class SingleConditionChallengeProgressCalculator implements ChallengeProgressCalculator {

    /**
     * Applies the filters declared by the challenge condition.
     */
    private final ChallengeMatchFilter matchFilter;

    /**
     * Creates the calculator.
     *
     * @param matchFilter condition match filter
     */
    protected SingleConditionChallengeProgressCalculator(ChallengeMatchFilter matchFilter) {
        this.matchFilter = matchFilter;
    }

    /**
     * Measures the single condition over the eligible matches, against the challenge's target.
     *
     * @param definition parsed challenge definition
     * @param context    weekly player context
     * @return normalized progress result
     */
    @Override
    public final ChallengeProgressResult calculate(
        ChallengeDefinition definition,
        PlayerChallengeContext context
    ) {
        ChallengeCondition condition = definition.singleCondition();

        List<PlayerMatch> eligibleMatches = context.playerMatches()
            .stream()
            .filter(playerMatch -> matchFilter.matches(playerMatch, condition))
            .toList();

        return ChallengeProgressResult.from(
            measure(condition, eligibleMatches),
            definition.progressTarget()
        );
    }

    /**
     * Measures the player's progress on the condition.
     *
     * @param condition       the challenge's single condition
     * @param eligibleMatches matches the condition's filters accept, in context order
     * @return current progress value
     */
    protected abstract BigDecimal measure(ChallengeCondition condition, List<PlayerMatch> eligibleMatches);
}

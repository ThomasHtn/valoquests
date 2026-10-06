package io.github.thomashtn.valoquests.challenge.calculator;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Calculates ratio-based weekly challenges.
 *
 * <p>The rate comes from {@link AggregateRateCalculator}, as totals over all eligible matches.
 */
@Component
public class RatioChallengeProgressCalculator
    implements ChallengeProgressCalculator {

    /**
     * Applies filters declared by challenge conditions.
     */
    private final ChallengeMatchFilter matchFilter;

    /**
     * Calculates the rate a metric takes over a set of matches.
     */
    private final AggregateRateCalculator rateCalculator;

    /**
     * Creates the ratio challenge-progress calculator.
     *
     * @param matchFilter    condition match filter
     * @param rateCalculator aggregate rate calculator
     */
    public RatioChallengeProgressCalculator(
        ChallengeMatchFilter matchFilter,
        AggregateRateCalculator rateCalculator
    ) {
        this.matchFilter = matchFilter;
        this.rateCalculator = rateCalculator;
    }

    /**
     * Returns the progress mode supported by this calculator.
     *
     * @return {@link ProgressMode#RATIO}
     */
    @Override
    public ProgressMode supportedMode() {
        return ProgressMode.RATIO;
    }

    /**
     * Calculates the ratio configured by the challenge definition.
     *
     * <p>The ratio shows before the minimum match count is reached, but cannot complete until it is.
     *
     * @param definition parsed challenge definition
     * @param context    weekly player context
     * @return normalized progress result
     */
    @Override
    public ChallengeProgressResult calculate(
        ChallengeDefinition definition,
        PlayerChallengeContext context
    ) {
        ChallengeCondition condition = definition.singleCondition();

        List<PlayerMatch> eligibleMatches = context.playerMatches()
            .stream()
            .filter(playerMatch ->
                matchFilter.matches(playerMatch, condition)
            )
            .toList();

        BigDecimal currentValue = rateCalculator
            .rateOf(condition.metric(), eligibleMatches)
            .orElse(BigDecimal.ZERO);

        ChallengeProgressResult normalizedResult =
            ChallengeProgressResult.from(
                currentValue,
                definition.progressTarget()
            );

        boolean minimumMatchesReached =
            eligibleMatches.size() >= condition.minimumMatches();

        return new ChallengeProgressResult(
            normalizedResult.currentValue(),
            normalizedResult.targetValue(),
            normalizedResult.completed() && minimumMatchesReached
        );
    }

}

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
 * <p>Checks a rate against a fixed threshold, where {@code BASELINE} checks it against the player's
 * own past. The rate itself is calculated by {@link AggregateRateCalculator}, from totals across all
 * eligible matches rather than from an average of per-match ratios, so both modes agree on what the
 * number means.</p>
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
     * <p>A ratio challenge may declare a minimum number of eligible matches.
     * The calculated ratio remains visible before this minimum is reached,
     * but the challenge cannot be completed until the match requirement is
     * satisfied.</p>
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

        validateCondition(condition);

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
                condition.target()
            );

        boolean minimumMatchesReached =
            eligibleMatches.size() >= condition.minimumMatches();

        return new ChallengeProgressResult(
            normalizedResult.currentValue(),
            normalizedResult.targetValue(),
            normalizedResult.progressPercentage(),
            normalizedResult.completed() && minimumMatchesReached
        );
    }

    /**
     * Validates the configuration required by a ratio challenge.
     *
     * @param condition challenge condition
     */
    private void validateCondition(
        ChallengeCondition condition
    ) {
        if (!rateCalculator.supports(condition.metric())) {
            throw new IllegalArgumentException(
                "Unsupported ratio metric: " + condition.metric()
            );
        }

        if (condition.minimumMatches() == null) {
            throw new IllegalArgumentException(
                "RATIO challenges require a minimum number of matches."
            );
        }

        if (condition.minimumMatches() <= 0) {
            throw new IllegalArgumentException(
                "The minimum number of matches must be greater than zero."
            );
        }
    }

}

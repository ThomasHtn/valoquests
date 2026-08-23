package io.github.thomashtn.valoquests.challenge.calculator;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Calculates challenges whose progress is the sum of one metric across all
 * eligible weekly matches.
 */
@Component
public class SumChallengeProgressCalculator
    implements ChallengeProgressCalculator {

    /**
     * Evaluates metric contributions for individual matches.
     */
    private final ChallengeMetricEvaluator metricEvaluator;

    /**
     * Applies filters declared by challenge conditions.
     */
    private final ChallengeMatchFilter matchFilter;

    /**
     * Creates the summed-progress calculator.
     *
     * @param metricEvaluator metric evaluator
     * @param matchFilter     condition match filter
     */
    public SumChallengeProgressCalculator(
        ChallengeMetricEvaluator metricEvaluator,
        ChallengeMatchFilter matchFilter
    ) {
        this.metricEvaluator = metricEvaluator;
        this.matchFilter = matchFilter;
    }

    /**
     * Returns the supported progress mode.
     *
     * @return {@link ProgressMode#SUM}
     */
    @Override
    public ProgressMode supportedMode() {
        return ProgressMode.SUM;
    }

    /**
     * Sums the selected metric across every eligible weekly match.
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

        BigDecimal currentValue = context.playerMatches()
            .stream()
            .filter(playerMatch ->
                matchFilter.matches(playerMatch, condition)
            )
            .map(playerMatch ->
                metricEvaluator.evaluate(
                    playerMatch,
                    condition.metric()
                )
            )
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ChallengeProgressResult.from(
            currentValue,
            condition.target()
        );
    }
}

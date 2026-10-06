package io.github.thomashtn.valoquests.challenge.calculator;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Calculates challenges whose progress is the sum of one metric across all
 * eligible weekly matches.
 */
@Component
public class SumChallengeProgressCalculator
    extends SingleConditionChallengeProgressCalculator {

    /**
     * Evaluates metric contributions for individual matches.
     */
    private final ChallengeMetricEvaluator metricEvaluator;

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
        super(matchFilter);
        this.metricEvaluator = metricEvaluator;
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
     * @param condition       the challenge's single condition
     * @param eligibleMatches matches the condition's filters accept
     * @return summed metric
     */
    @Override
    protected BigDecimal measure(ChallengeCondition condition, List<PlayerMatch> eligibleMatches) {
        return metricEvaluator.sumOf(condition.metric(), eligibleMatches);
    }
}

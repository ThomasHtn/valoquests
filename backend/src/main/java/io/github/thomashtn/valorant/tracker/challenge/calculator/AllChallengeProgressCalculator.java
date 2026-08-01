package io.github.thomashtn.valorant.tracker.challenge.calculator;

import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeCondition;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valorant.tracker.challenge.model.ProgressMode;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Calculates composite challenges requiring every configured condition to be
 * completed.
 *
 * <p>Each condition is calculated independently and capped at its own target.
 * This prevents excessive progress on one condition from compensating for an
 * incomplete condition.</p>
 */
@Component
public class AllChallengeProgressCalculator
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
     * Creates the composite all-conditions calculator.
     *
     * @param metricEvaluator metric evaluator
     * @param matchFilter     condition match filter
     */
    public AllChallengeProgressCalculator(
        ChallengeMetricEvaluator metricEvaluator,
        ChallengeMatchFilter matchFilter
    ) {
        this.metricEvaluator = metricEvaluator;
        this.matchFilter = matchFilter;
    }

    /**
     * Returns the supported progress mode.
     *
     * @return {@link ProgressMode#ALL}
     */
    @Override
    public ProgressMode supportedMode() {
        return ProgressMode.ALL;
    }

    /**
     * Calculates every condition independently and combines their normalized
     * progress.
     *
     * <p>The current value of each condition is capped at its target before
     * being added to the global result. Consequently, the global target can
     * only be reached when every condition is complete.</p>
     *
     * @param definition parsed challenge definition
     * @param context    weekly player context
     * @return combined progress result
     */
    @Override
    public ChallengeProgressResult calculate(
        ChallengeDefinition definition,
        PlayerChallengeContext context
    ) {
        BigDecimal currentValue = BigDecimal.ZERO;
        BigDecimal targetValue = BigDecimal.ZERO;

        for (ChallengeCondition condition : definition.conditions()) {
            BigDecimal conditionValue = calculateConditionValue(
                condition,
                context
            );

            currentValue = currentValue.add(
                conditionValue.min(condition.target())
            );

            targetValue = targetValue.add(condition.target());
        }

        return ChallengeProgressResult.from(
            currentValue,
            targetValue
        );
    }

    /**
     * Calculates the accumulated metric value for one condition.
     *
     * @param condition challenge condition
     * @param context   weekly player context
     * @return accumulated condition value
     */
    private BigDecimal calculateConditionValue(
        ChallengeCondition condition,
        PlayerChallengeContext context
    ) {
        return context.playerMatches()
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
    }
}

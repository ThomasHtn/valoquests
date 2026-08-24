package io.github.thomashtn.valoquests.challenge.calculator;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Counts weekly matches that independently satisfy one challenge condition.
 */
@Component
public class CountMatchesChallengeProgressCalculator
    implements ChallengeProgressCalculator {

    /**
     * Evaluates the configured metric for individual matches.
     */
    private final ChallengeMetricEvaluator metricEvaluator;

    /**
     * Applies the common game-mode filters.
     */
    private final ChallengeMatchFilter matchFilter;

    /**
     * Creates the matching-occurrence calculator.
     *
     * @param metricEvaluator metric evaluator
     * @param matchFilter     condition match filter
     */
    public CountMatchesChallengeProgressCalculator(
        ChallengeMetricEvaluator metricEvaluator,
        ChallengeMatchFilter matchFilter
    ) {
        this.metricEvaluator = metricEvaluator;
        this.matchFilter = matchFilter;
    }

    /**
     * Returns the supported progress mode.
     *
     * @return {@link ProgressMode#COUNT_MATCHES}
     */
    @Override
    public ProgressMode supportedMode() {
        return ProgressMode.COUNT_MATCHES;
    }

    /**
     * Counts matches whose metric reaches the configured per-match target.
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

        long matchingMatches = context.playerMatches()
            .stream()
            .filter(playerMatch ->
                matchFilter.matches(playerMatch, condition)
            )
            .filter(playerMatch -> conditionMatches(
                metricEvaluator.evaluate(
                    playerMatch,
                    condition.metric()
                ),
                condition
            ))
            .count();

        BigDecimal occurrenceTarget = BigDecimal.valueOf(
            condition.occurrences()
        );

        return ChallengeProgressResult.from(
            BigDecimal.valueOf(matchingMatches),
            occurrenceTarget
        );
    }

    /**
     * Applies the configured operator to one evaluated match value.
     *
     * @param currentValue metric value produced by the match
     * @param condition    challenge condition
     * @return {@code true} when the match satisfies the condition
     */
    private boolean conditionMatches(
        BigDecimal currentValue,
        ChallengeCondition condition
    ) {
        // ChallengeOperator declares GTE alone, so there is no other comparison to dispatch on.
        return currentValue.compareTo(condition.target()) >= 0;
    }
}

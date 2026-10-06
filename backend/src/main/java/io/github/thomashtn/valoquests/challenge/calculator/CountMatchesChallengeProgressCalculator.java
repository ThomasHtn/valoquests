package io.github.thomashtn.valoquests.challenge.calculator;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Counts weekly matches that independently satisfy one challenge condition.
 */
@Component
public class CountMatchesChallengeProgressCalculator
    extends SingleConditionChallengeProgressCalculator {

    /**
     * Evaluates the configured metric for individual matches.
     */
    private final ChallengeMetricEvaluator metricEvaluator;

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
        super(matchFilter);
        this.metricEvaluator = metricEvaluator;
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
     * @param condition       the challenge's single condition
     * @param eligibleMatches matches the condition's filters accept
     * @return number of matches meeting the condition
     */
    @Override
    protected BigDecimal measure(ChallengeCondition condition, List<PlayerMatch> eligibleMatches) {
        long matchingMatches = eligibleMatches.stream()
            .filter(playerMatch -> condition.isMetBy(
                metricEvaluator.evaluate(playerMatch, condition.metric())
            ))
            .count();

        return BigDecimal.valueOf(matchingMatches);
    }
}

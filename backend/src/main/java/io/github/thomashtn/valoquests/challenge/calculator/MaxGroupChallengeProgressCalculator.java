package io.github.thomashtn.valoquests.challenge.calculator;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Calculates challenges whose progress corresponds to the highest accumulated
 * metric value found within a single group of eligible matches.
 *
 * <p>For example, a challenge requiring several competitive matches with the
 * same agent groups matches by agent and returns the size of the largest
 * group.</p>
 */
@Component
public class MaxGroupChallengeProgressCalculator
    extends SingleConditionChallengeProgressCalculator {

    /**
     * Extracts the metric value contributed by each eligible match.
     */
    private final ChallengeMetricEvaluator metricEvaluator;

    /**
     * Reads the key each match is grouped under.
     */
    private final MatchGroupKeyExtractor keyExtractor;

    /**
     * Creates the maximum-group challenge-progress calculator.
     *
     * @param metricEvaluator metric evaluator
     * @param matchFilter     condition match filter
     * @param weekCalendar    calendar resolving the day a match belongs to
     */
    public MaxGroupChallengeProgressCalculator(
        ChallengeMetricEvaluator metricEvaluator,
        ChallengeMatchFilter matchFilter,
        WeekCalendar weekCalendar
    ) {
        super(matchFilter);
        this.metricEvaluator = metricEvaluator;
        this.keyExtractor = new MatchGroupKeyExtractor(weekCalendar);
    }

    /**
     * Returns the progress mode supported by this calculator.
     *
     * @return {@link ProgressMode#MAX_GROUP}
     */
    @Override
    public ProgressMode supportedMode() {
        return ProgressMode.MAX_GROUP;
    }

    /**
     * Groups eligible matches according to the configured dimension and
     * returns the highest accumulated metric value found in one group.
     *
     * @param condition       the challenge's single condition
     * @param eligibleMatches matches the condition's filters accept
     * @return highest metric total of one group
     */
    @Override
    protected BigDecimal measure(ChallengeCondition condition, List<PlayerMatch> eligibleMatches) {
        Map<Object, BigDecimal> groupedValues = eligibleMatches.stream()
            .map(playerMatch -> new GroupedMetricValue(
                keyExtractor.keyOf(playerMatch, condition.groupBy()),
                metricEvaluator.evaluate(
                    playerMatch,
                    condition.metric()
                )
            ))
            .filter(groupedValue ->
                groupedValue.groupValue() != null
            )
            .collect(Collectors.toMap(
                GroupedMetricValue::groupValue,
                GroupedMetricValue::metricValue,
                BigDecimal::add
            ));

        return groupedValues
            .values()
            .stream()
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
    }

    /**
     * Associates one grouping value with the metric contributed by a match.
     *
     * @param groupValue  grouping key
     * @param metricValue contributed metric value
     */
    private record GroupedMetricValue(
        Object groupValue,
        BigDecimal metricValue
    ) {
    }
}

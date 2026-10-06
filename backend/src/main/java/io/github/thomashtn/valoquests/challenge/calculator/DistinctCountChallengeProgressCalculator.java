package io.github.thomashtn.valoquests.challenge.calculator;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeMetric;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Calculates challenges whose progress is the number of distinct values found
 * across eligible weekly matches.
 */
@Component
public class DistinctCountChallengeProgressCalculator
    extends SingleConditionChallengeProgressCalculator {

    /**
     * Evaluates whether a match contributes to the configured metric.
     */
    private final ChallengeMetricEvaluator metricEvaluator;

    /**
     * Reads the key each match is grouped under.
     */
    private final MatchGroupKeyExtractor keyExtractor;

    /**
     * Creates the distinct-value calculator.
     *
     * @param metricEvaluator metric evaluator
     * @param matchFilter     condition match filter
     * @param weekCalendar    calendar resolving the day a match belongs to
     */
    public DistinctCountChallengeProgressCalculator(
        ChallengeMetricEvaluator metricEvaluator,
        ChallengeMatchFilter matchFilter,
        WeekCalendar weekCalendar
    ) {
        super(matchFilter);
        this.metricEvaluator = metricEvaluator;
        this.keyExtractor = new MatchGroupKeyExtractor(weekCalendar);
    }

    /**
     * Returns the supported progress mode.
     *
     * @return {@link ProgressMode#DISTINCT_COUNT}
     */
    @Override
    public ProgressMode supportedMode() {
        return ProgressMode.DISTINCT_COUNT;
    }

    /**
     * Counts distinct grouping values among matches contributing to the
     * configured metric.
     *
     * @param condition       the challenge's single condition
     * @param eligibleMatches matches the condition's filters accept
     * @return number of distinct grouping values
     */
    @Override
    protected BigDecimal measure(ChallengeCondition condition, List<PlayerMatch> eligibleMatches) {
        long distinctValues = eligibleMatches.stream()
            .filter(playerMatch -> contributes(playerMatch, condition))
            .map(playerMatch -> keyExtractor.keyOf(playerMatch, condition.groupBy()))
            .filter(Objects::nonNull)
            .distinct()
            .count();

        return BigDecimal.valueOf(distinctValues);
    }

    /**
     * Determines whether one eligible match contributes to the distinct set.
     *
     * <p>The play-day metric is represented by the grouping key itself. Other
     * metrics must produce a strictly positive value, which notably excludes
     * losses from challenges based on {@code MATCHES_WON}.</p>
     *
     * @param playerMatch persisted player-match data
     * @param condition   parsed challenge condition
     * @return {@code true} when the match contributes to progress
     */
    private boolean contributes(
        PlayerMatch playerMatch,
        ChallengeCondition condition
    ) {
        if (condition.metric() == ChallengeMetric.PLAY_DAY) {
            return true;
        }

        return metricEvaluator
            .evaluate(playerMatch, condition.metric())
            .signum() > 0;
    }
}

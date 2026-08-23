package io.github.thomashtn.valoquests.challenge.calculator;

import io.github.thomashtn.valoquests.challenge.model.ChallengeCondition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeGroupBy;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
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
    implements ChallengeProgressCalculator {

    /**
     * Extracts the metric value contributed by each eligible match.
     */
    private final ChallengeMetricEvaluator metricEvaluator;

    /**
     * Applies the filters declared by the challenge condition.
     */
    private final ChallengeMatchFilter matchFilter;

    /**
     * Calendar placing a match on the calendar day it counts towards.
     */
    private final WeekCalendar weekCalendar;

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
        this.metricEvaluator = metricEvaluator;
        this.matchFilter = matchFilter;
        this.weekCalendar = weekCalendar;
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
        ChallengeGroupBy groupBy = condition.groupBy();

        if (groupBy == null) {
            throw new IllegalArgumentException(
                "MAX_GROUP challenges require a grouping dimension."
            );
        }

        Map<Object, BigDecimal> groupedValues = context.playerMatches()
            .stream()
            .filter(playerMatch ->
                matchFilter.matches(playerMatch, condition)
            )
            .map(playerMatch -> new GroupedMetricValue(
                extractGroupValue(playerMatch, groupBy),
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

        BigDecimal maximumGroupValue = groupedValues
            .values()
            .stream()
            .filter(Objects::nonNull)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);

        return ChallengeProgressResult.from(
            maximumGroupValue,
            condition.target()
        );
    }

    /**
     * Extracts the value used to group one player match.
     *
     * @param playerMatch persisted player-match data
     * @param groupBy     requested grouping dimension
     * @return grouping value, or {@code null} when unavailable
     */
    private Object extractGroupValue(
        PlayerMatch playerMatch,
        ChallengeGroupBy groupBy
    ) {
        return switch (groupBy) {
            case AGENT -> extractAgentValue(playerMatch);
            case GAME_MODE -> playerMatch.getMatch().getGameMode();
            case PLAY_DAY -> weekCalendar.dayOf(
                playerMatch.getMatch().getStartedAt()
            );
        };
    }

    /**
     * Returns the most stable available agent identifier.
     *
     * @param playerMatch persisted player-match data
     * @return agent identifier or fallback name
     */
    private String extractAgentValue(PlayerMatch playerMatch) {
        if (playerMatch.getAgentId() != null
            && !playerMatch.getAgentId().isBlank()) {
            return playerMatch.getAgentId();
        }

        if (playerMatch.getAgentName() != null
            && !playerMatch.getAgentName().isBlank()) {
            return playerMatch.getAgentName();
        }

        return null;
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

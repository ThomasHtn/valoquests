package io.github.thomashtn.valoquests.challenge.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

/**
 * Represents one typed condition extracted from a challenge JSON definition.
 *
 * @param metric         statistic evaluated by the condition
 * @param operator       comparison operator applied to the calculated value
 * @param target         target value required by the condition
 * @param gameMode       optional game-mode filter
 * @param groupBy        optional grouping dimension
 * @param scope          optional evaluation scope
 * @param occurrences    optional number of matching occurrences required
 * @param streak         optional consecutive-match target
 * @param minimumMatches optional minimum number of eligible matches
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChallengeCondition(

    ChallengeMetric metric,
    ChallengeOperator operator,
    BigDecimal target,
    ChallengeGameMode gameMode,
    ChallengeGroupBy groupBy,
    ChallengeScope scope,
    Integer occurrences,
    Integer streak,
    Integer minimumMatches
) {

    /**
     * Returns the effective game-mode filter.
     *
     * <p>Definitions without an explicit game mode are treated as applying to
     * every game mode.</p>
     *
     * @return configured mode or {@link ChallengeGameMode#ANY}
     */
    public ChallengeGameMode effectiveGameMode() {
        return gameMode == null ? ChallengeGameMode.ANY : gameMode;
    }

    /**
     * Tells whether the metric is a rate rather than a count.
     *
     * <p>A rate is what a player holds, not what they accumulate: it scales on a talent anchor and
     * keeps its decimals, where a count scales on volume and rounds to a readable step.
     *
     * @return {@code true} for kill-to-death, per-round and headshot-rate metrics
     */
    public boolean isRateMetric() {
        return metric == ChallengeMetric.KD
            || metric == ChallengeMetric.ADR
            || metric == ChallengeMetric.ACS
            || metric == ChallengeMetric.HEADSHOT_RATE;
    }

    /**
     * Tells whether the metric is a ratio that keeps its decimals once resolved.
     *
     * <p>Per-round averages are rates too, but they read as whole numbers: an ADR of 155, never
     * 155.76.
     *
     * @return {@code true} for kill-to-death and headshot-rate metrics
     */
    public boolean isRatioMetric() {
        return metric == ChallengeMetric.KD || metric == ChallengeMetric.HEADSHOT_RATE;
    }

    /**
     * Tells whether the metric counts matches rather than what happened in them.
     *
     * @return {@code true} for matches played and matches won
     */
    public boolean isMatchCountMetric() {
        return metric == ChallengeMetric.MATCHES_PLAYED
            || metric == ChallengeMetric.MATCHES_WON;
    }

    /**
     * Returns a copy with new numeric fields, every filter untouched.
     *
     * @param newTarget         resolved target
     * @param newOccurrences    resolved occurrences, or {@code null} when none were declared
     * @param newMinimumMatches resolved minimum, or {@code null} when none was declared
     * @return resolved condition
     */
    public ChallengeCondition withNumbers(
        BigDecimal newTarget,
        Integer newOccurrences,
        Integer newMinimumMatches
    ) {
        return new ChallengeCondition(
            metric,
            operator,
            newTarget,
            gameMode,
            groupBy,
            scope,
            newOccurrences,
            streak,
            newMinimumMatches
        );
    }
}

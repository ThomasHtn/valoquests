package io.github.thomashtn.valoquests.challenge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
// Rows stored before the derived is*Metric flags were removed still carry them.
@JsonIgnoreProperties(ignoreUnknown = true)
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
     * Tells whether a value clears this condition's target.
     *
     * @param value value one match or one group produced
     * @return {@code true} when the value satisfies the operator
     */
    public boolean isMetBy(BigDecimal value) {
        // ChallengeOperator declares GTE alone, so there is no other comparison to dispatch on.
        return value.compareTo(target) >= 0;
    }
}

package io.github.thomashtn.valoquests.challenge.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Represents a validated challenge definition ready to be evaluated.
 *
 * @param progressMode calculation strategy
 * @param conditions   immutable list of challenge conditions
 */
public record ChallengeDefinition(

    ProgressMode progressMode,
    List<ChallengeCondition> conditions
) {

    /**
     * Creates an immutable and validated challenge definition.
     */
    public ChallengeDefinition {
        Objects.requireNonNull(
            progressMode,
            "Challenge progress mode must not be null."
        );
        Objects.requireNonNull(
            conditions,
            "Challenge conditions must not be null."
        );

        conditions = List.copyOf(conditions);

        if (conditions.isEmpty()) {
            throw new IllegalArgumentException(
                "A challenge must contain at least one condition."
            );
        }
    }

    /**
     * Returns the first condition of a single-condition challenge.
     *
     * @return first configured condition
     * @throws IllegalStateException when several conditions are configured
     */
    public ChallengeCondition singleCondition() {
        if (conditions.size() != 1) {
            throw new IllegalStateException(
                "Expected exactly one challenge condition but found "
                    + conditions.size()
                    + "."
            );
        }

        return conditions.getFirst();
    }

    /**
     * Returns the value calculators compare a player's progress against.
     *
     * <p>Not always the condition's target: occurrences, streak length or, for a composite, the sum of
     * targets. The interface draws its progress bar against it.
     *
     * @return progress target
     */
    public BigDecimal progressTarget() {
        return switch (progressMode) {
            case ALL -> conditions.stream()
                .map(ChallengeCondition::target)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            case COUNT_MATCHES -> BigDecimal.valueOf(singleCondition().occurrences());
            case MAX_STREAK -> BigDecimal.valueOf(singleCondition().streak());
            case SUM, DISTINCT_COUNT, MAX_GROUP, RATIO -> singleCondition().target();
        };
    }

    /**
     * Tells whether completing this challenge requires ranked matches.
     *
     * @return {@code true} when any condition filters on competitive only
     */
    public boolean isCompetitiveOnly() {
        return conditions.stream()
            .anyMatch(condition -> condition.effectiveGameMode().isCompetitiveOnly());
    }
}

package io.github.thomashtn.valoquests.challenge.model;

import java.util.List;
import java.util.Objects;

/**
 * Represents a validated challenge definition ready to be evaluated.
 *
 * @param schemaVersion version of the JSON rule schema
 * @param progressMode  calculation strategy
 * @param conditions    immutable list of challenge conditions
 */
public record ChallengeDefinition(

    int schemaVersion,
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

        if (schemaVersion <= 0) {
            throw new IllegalArgumentException(
                "Challenge schema version must be greater than zero."
            );
        }

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
}

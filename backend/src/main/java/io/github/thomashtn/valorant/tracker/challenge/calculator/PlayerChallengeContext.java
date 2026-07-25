package io.github.thomashtn.valorant.tracker.challenge.calculator;

import io.github.thomashtn.valorant.tracker.match.entity.PlayerMatch;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Contains the persisted player data required to evaluate challenges during
 * one weekly period.
 *
 * @param playerId      internal player identifier
 * @param weekStart     first calendar day of the evaluated week
 * @param periodStart   inclusive UTC beginning of the evaluated period
 * @param periodEnd     exclusive UTC end of the evaluated period
 * @param playerMatches immutable chronological list of eligible matches
 */
public record PlayerChallengeContext(

    Long playerId,
    LocalDate weekStart,
    Instant periodStart,
    Instant periodEnd,
    List<PlayerMatch> playerMatches
) {

    /**
     * Creates an immutable and validated calculation context.
     */
    public PlayerChallengeContext {
        Objects.requireNonNull(
            playerId,
            "Player identifier must not be null."
        );
        Objects.requireNonNull(
            weekStart,
            "Week start must not be null."
        );
        Objects.requireNonNull(
            periodStart,
            "Period start must not be null."
        );
        Objects.requireNonNull(
            periodEnd,
            "Period end must not be null."
        );
        Objects.requireNonNull(
            playerMatches,
            "Player matches must not be null."
        );

        if (!periodStart.isBefore(periodEnd)) {
            throw new IllegalArgumentException(
                "Challenge period start must be before its end."
            );
        }

        playerMatches = List.copyOf(playerMatches);
    }
}

package io.github.thomashtn.valoquests.challenge.calculator;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Contains the persisted player data required to evaluate challenges during
 * one weekly period.
 *
 * @param playerId        internal player identifier
 * @param weekStart       first calendar day of the evaluated week
 * @param periodStart     inclusive UTC beginning of the evaluated period
 * @param periodEnd       exclusive UTC end of the evaluated period
 * @param playerMatches   immutable chronological list of eligible matches
 * @param baselineMatches immutable chronological list of the matches preceding the evaluated week,
 *     used by challenges that ask a player to improve on their own recent form
 */
public record PlayerChallengeContext(

    Long playerId,
    LocalDate weekStart,
    Instant periodStart,
    Instant periodEnd,
    List<PlayerMatch> playerMatches,
    List<PlayerMatch> baselineMatches
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
        Objects.requireNonNull(
            baselineMatches,
            "Baseline matches must not be null."
        );

        if (!periodStart.isBefore(periodEnd)) {
            throw new IllegalArgumentException(
                "Challenge period start must be before its end."
            );
        }

        playerMatches = List.copyOf(playerMatches);
        baselineMatches = List.copyOf(baselineMatches);
    }

    /**
     * Creates a context carrying no baseline window.
     *
     * <p>Every challenge but the ones comparing a player to their own past ignores the baseline, so
     * this keeps their call sites — and their tests — free of a window they never read.
     *
     * @param playerId      internal player identifier
     * @param weekStart     first calendar day of the evaluated week
     * @param periodStart   inclusive UTC beginning of the evaluated period
     * @param periodEnd     exclusive UTC end of the evaluated period
     * @param playerMatches immutable chronological list of eligible matches
     */
    public PlayerChallengeContext(
        Long playerId,
        LocalDate weekStart,
        Instant periodStart,
        Instant periodEnd,
        List<PlayerMatch> playerMatches
    ) {
        this(playerId, weekStart, periodStart, periodEnd, playerMatches, List.of());
    }

    /**
     * Creates a copy of this context truncated to the first matches, in chronological order.
     *
     * <p>Used to replay a calculator match by match, to find the exact match at which a challenge
     * became completed, without changing the calculator implementations themselves.
     *
     * <p>The baseline window is carried over untouched: it describes weeks that are already over, so
     * truncating it would make a challenge's target move as the replay advances.
     *
     * @param matchCount number of leading matches to keep, must be between 1 and {@link #playerMatches()}'s size
     * @return truncated context sharing every other field
     */
    public PlayerChallengeContext prefixedTo(int matchCount) {
        return new PlayerChallengeContext(
            playerId,
            weekStart,
            periodStart,
            periodEnd,
            playerMatches.subList(0, matchCount),
            baselineMatches
        );
    }
}

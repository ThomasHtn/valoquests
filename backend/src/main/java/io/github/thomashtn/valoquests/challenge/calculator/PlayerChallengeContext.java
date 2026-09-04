package io.github.thomashtn.valoquests.challenge.calculator;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Contains the persisted player data required to evaluate challenges over one period: a week for
 * the weekly pack, a single day for the daily challenge.
 *
 * @param playerId        internal player identifier
 * @param weekStart       Monday of the week the period belongs to
 * @param periodStart     inclusive beginning of the evaluated period
 * @param periodEnd       exclusive end of the evaluated period
 * @param playerMatches   immutable chronological list of eligible matches
 * @param baselineMatches immutable chronological list of the matches preceding the evaluated week,
 *     read only by the dormant baseline progress mode and empty everywhere else
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
     * @param playerId      internal player identifier
     * @param weekStart     Monday of the week the period belongs to
     * @param periodStart   inclusive beginning of the evaluated period
     * @param periodEnd     exclusive end of the evaluated period
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

    /**
     * Creates a copy of this context narrowed to a sub-period.
     *
     * <p>How a day's challenge is evaluated from the week's matches without a second query: the
     * daily window always lies inside the weekly one, so filtering in memory is exact.
     *
     * @param subPeriodStart inclusive beginning of the sub-period
     * @param subPeriodEnd   exclusive end of the sub-period
     * @return narrowed context sharing the player and week
     */
    public PlayerChallengeContext restrictedTo(Instant subPeriodStart, Instant subPeriodEnd) {
        List<PlayerMatch> matches = playerMatches.stream()
            .filter(playerMatch -> {
                Instant startedAt = playerMatch.getMatch().getStartedAt();

                return !startedAt.isBefore(subPeriodStart) && startedAt.isBefore(subPeriodEnd);
            })
            .toList();

        return new PlayerChallengeContext(
            playerId,
            weekStart,
            subPeriodStart,
            subPeriodEnd,
            matches,
            baselineMatches
        );
    }
}

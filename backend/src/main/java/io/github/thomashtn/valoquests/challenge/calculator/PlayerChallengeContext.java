package io.github.thomashtn.valoquests.challenge.calculator;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Contains the persisted player data required to evaluate challenges over one period: a week for
 * the weekly pack, a single day for the daily challenge.
 *
 * @param playerMatches immutable chronological list of eligible matches
 */
public record PlayerChallengeContext(List<PlayerMatch> playerMatches) {

    /**
     * Creates an immutable and validated calculation context.
     */
    public PlayerChallengeContext {
        Objects.requireNonNull(
            playerMatches,
            "Player matches must not be null."
        );

        playerMatches = List.copyOf(playerMatches);
    }

    /**
     * Creates a copy of this context narrowed to a sub-period.
     *
     * <p>How a day's challenge is evaluated from the week's matches without a second query: the
     * daily window always lies inside the weekly one, so filtering in memory is exact.
     *
     * @param subPeriodStart inclusive beginning of the sub-period
     * @param subPeriodEnd   exclusive end of the sub-period
     * @return narrowed context
     */
    public PlayerChallengeContext restrictedTo(Instant subPeriodStart, Instant subPeriodEnd) {
        return new PlayerChallengeContext(playerMatches.stream()
            .filter(playerMatch -> {
                Instant startedAt = playerMatch.getMatch().getStartedAt();

                return !startedAt.isBefore(subPeriodStart) && startedAt.isBefore(subPeriodEnd);
            })
            .toList());
    }
}

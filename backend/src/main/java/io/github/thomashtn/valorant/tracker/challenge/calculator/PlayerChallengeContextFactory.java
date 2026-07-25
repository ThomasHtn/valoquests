package io.github.thomashtn.valorant.tracker.challenge.calculator;

import io.github.thomashtn.valorant.tracker.match.entity.PlayerMatch;
import io.github.thomashtn.valorant.tracker.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

/**
 * Builds challenge-calculation contexts from matches already persisted in the
 * application database.
 */
@Component
public class PlayerChallengeContextFactory {

    /**
     * Repository used to read the player's persisted weekly matches.
     */
    private final PlayerMatchRepository playerMatchRepository;

    /**
     * Creates the context factory.
     *
     * @param playerMatchRepository player-match repository
     */
    public PlayerChallengeContextFactory(
        PlayerMatchRepository playerMatchRepository
    ) {
        this.playerMatchRepository = playerMatchRepository;
    }

    /**
     * Creates the challenge context for one player and one week.
     *
     * <p>The supplied date must represent the Monday beginning the week.
     * Dates are converted to UTC because match timestamps are persisted as
     * {@link Instant} values.</p>
     *
     * @param player    player whose challenges must be evaluated
     * @param weekStart Monday beginning the evaluated week
     * @return immutable calculation context
     */
    @Transactional(readOnly = true)
    public PlayerChallengeContext create(
        Player player,
        LocalDate weekStart
    ) {
        Objects.requireNonNull(player, "Player must not be null.");
        Objects.requireNonNull(
            weekStart,
            "Week start must not be null."
        );

        if (player.getId() == null) {
            throw new IllegalArgumentException(
                "The player must be persisted before building a context."
            );
        }

        Instant periodStart = weekStart
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant();

        Instant periodEnd = weekStart
            .plusWeeks(1)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant();

        List<PlayerMatch> playerMatches =
            playerMatchRepository.findForChallengePeriod(
                player.getId(),
                periodStart,
                periodEnd
            );

        return new PlayerChallengeContext(
            player.getId(),
            weekStart,
            periodStart,
            periodEnd,
            playerMatches
        );
    }
}

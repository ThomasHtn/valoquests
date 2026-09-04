package io.github.thomashtn.valoquests.challenge.calculator;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.repository.PlayerMatchRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds challenge-calculation contexts from matches already persisted in the
 * application database.
 *
 * <p>Loads the week only. A day's context is carved out of the week's with
 * {@link PlayerChallengeContext#restrictedTo(Instant, Instant)}, and no baseline window is loaded
 * any more: the catalogue declares no baseline challenge, and four extra weeks of matches per
 * player per recalculation bought nothing.
 */
@Component
public class PlayerChallengeContextFactory {

    /**
     * Repository used to read the player's persisted weekly matches.
     */
    private final PlayerMatchRepository playerMatchRepository;

    /**
     * Calendar resolving the instants a week spans.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the context factory.
     *
     * @param playerMatchRepository player-match repository
     * @param weekCalendar          calendar resolving the instants a week spans
     */
    public PlayerChallengeContextFactory(
        PlayerMatchRepository playerMatchRepository,
        WeekCalendar weekCalendar
    ) {
        this.playerMatchRepository = playerMatchRepository;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Creates the challenge context for one player and one week.
     *
     * <p>The supplied date must represent the Monday beginning the week. It is resolved to the
     * half-open instant range the week spans, because match timestamps are persisted as
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

        Instant periodStart = weekCalendar.startOf(weekStart);
        Instant periodEnd = weekCalendar.endOf(weekStart);

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

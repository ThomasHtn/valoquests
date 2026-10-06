package io.github.thomashtn.valoquests.challenge.calculator;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the windows {@link PlayerChallengeContext} can be narrowed to.
 */
class PlayerChallengeContextTest {

    /**
     * Verifies that a day is carved out of the week with half-open bounds, in order.
     */
    @Test
    void shouldRestrictTheWeekToOneDay() {
        PlayerMatch beforeMidnight = matchAt("2026-07-21T23:59:59Z");
        PlayerMatch atMidnight = matchAt("2026-07-22T00:00:00Z");
        PlayerMatch evening = matchAt("2026-07-22T21:00:00Z");
        PlayerMatch nextDay = matchAt("2026-07-23T00:00:00Z");
        PlayerChallengeContext week =
            new PlayerChallengeContext(List.of(beforeMidnight, atMidnight, evening, nextDay));

        PlayerChallengeContext day = week.restrictedTo(
            Instant.parse("2026-07-22T00:00:00Z"),
            Instant.parse("2026-07-23T00:00:00Z")
        );

        assertThat(day.playerMatches()).containsExactly(atMidnight, evening);
    }

    /**
     * Creates a player match started at one instant.
     *
     * @param startedAt match start
     * @return player match fixture
     */
    private PlayerMatch matchAt(String startedAt) {
        ValorantMatch match = new ValorantMatch();
        match.setStartedAt(Instant.parse(startedAt));

        PlayerMatch playerMatch = new PlayerMatch();
        playerMatch.setMatch(match);
        return playerMatch;
    }
}

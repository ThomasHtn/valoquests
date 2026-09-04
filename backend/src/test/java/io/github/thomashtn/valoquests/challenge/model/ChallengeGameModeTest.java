package io.github.thomashtn.valoquests.challenge.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.match.model.GameMode;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Unit tests for {@link ChallengeGameMode}.
 */
class ChallengeGameModeTest {

    /**
     * Verifies that every filter lets at least one game mode through.
     *
     * <p>A filter matching nothing would define challenges that can never progress, quietly
     * freezing them at zero for every player.
     *
     * @param filter challenge filter under test
     */
    @ParameterizedTest
    @EnumSource(ChallengeGameMode.class)
    void shouldDesignateAtLeastOneGameMode(ChallengeGameMode filter) {
        assertThat(Arrays.stream(GameMode.values()).anyMatch(filter::matches)).isTrue();
    }

    /**
     * Verifies that no filter lets a mode through that synchronization does not import.
     *
     * @param filter challenge filter under test
     */
    @ParameterizedTest
    @EnumSource(
        value = ChallengeGameMode.class,
        names = "ANY",
        mode = EnumSource.Mode.EXCLUDE
    )
    void shouldOnlyDesignateImportedGameModes(ChallengeGameMode filter) {
        assertThat(Arrays.stream(GameMode.values()).filter(filter::matches))
            .allMatch(GameMode::isImportEligible);
    }

    /**
     * Verifies that the unfiltered value accepts every game mode.
     */
    @Test
    void shouldAcceptEveryGameModeWhenUnfiltered() {
        assertThat(GameMode.values())
            .allMatch(ChallengeGameMode.ANY::matches);
    }

    /**
     * Verifies that a single-mode filter rejects the other game modes.
     */
    @Test
    void shouldRejectOtherGameModesWhenFiltered() {
        assertThat(ChallengeGameMode.DEATHMATCH.matches(GameMode.TEAM_DEATHMATCH)).isFalse();
        assertThat(ChallengeGameMode.DEATHMATCH.matches(GameMode.DEATHMATCH)).isTrue();
        assertThat(ChallengeGameMode.UNRATED.matches(GameMode.COMPETITIVE)).isFalse();
        assertThat(ChallengeGameMode.UNRATED.matches(GameMode.UNRATED)).isTrue();
    }

    /**
     * Verifies that the long-format filter is an explicit list: competitive and unrated, and not
     * the other round-based modes, Premier included.
     */
    @Test
    void shouldLetUnratedThroughTheLongFormatFilterOnly() {
        assertThat(ChallengeGameMode.COMPETITIVE_OR_UNRATED.matches(GameMode.UNRATED)).isTrue();
        assertThat(ChallengeGameMode.COMPETITIVE_OR_UNRATED.matches(GameMode.COMPETITIVE)).isTrue();
        assertThat(ChallengeGameMode.COMPETITIVE.matches(GameMode.UNRATED)).isFalse();

        assertThat(Arrays.stream(GameMode.values()).filter(ChallengeGameMode.COMPETITIVE_OR_UNRATED::matches))
            .containsExactlyInAnyOrder(GameMode.COMPETITIVE, GameMode.UNRATED);
    }

    /**
     * Verifies that only the competitive filter is reported as ranked-only.
     */
    @Test
    void shouldFlagTheCompetitiveFilterOnlyAsCompetitiveOnly() {
        assertThat(Arrays.stream(ChallengeGameMode.values()).filter(ChallengeGameMode::isCompetitiveOnly))
            .containsExactly(ChallengeGameMode.COMPETITIVE);
    }

    /**
     * Verifies that an unresolved game mode never satisfies a filtered value.
     */
    @Test
    void shouldRejectNullGameModeWhenFiltered() {
        assertThat(ChallengeGameMode.COMPETITIVE.matches(null)).isFalse();
        assertThat(ChallengeGameMode.ANY.matches(null)).isTrue();
    }
}

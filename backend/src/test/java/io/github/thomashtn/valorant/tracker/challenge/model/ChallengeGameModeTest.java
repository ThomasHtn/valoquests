package io.github.thomashtn.valorant.tracker.challenge.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valorant.tracker.match.model.GameMode;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Unit tests for {@link ChallengeGameMode}.
 */
class ChallengeGameModeTest {

    /**
     * Verifies that every challenge filter still designates an existing game mode.
     *
     * <p>{@link ChallengeGameMode#matches(GameMode)} pairs the two enums by name, which the compiler
     * cannot check. Renaming or removing a {@link GameMode} constant would silently turn the
     * corresponding filter into one that never matches, quietly freezing the affected challenges at
     * zero progress.
     *
     * @param filter challenge filter under test
     */
    @ParameterizedTest
    @EnumSource(
        value = ChallengeGameMode.class,
        names = "ANY",
        mode = EnumSource.Mode.EXCLUDE
    )
    void shouldDesignateAnExistingGameMode(ChallengeGameMode filter) {
        assertThat(Arrays.stream(GameMode.values()).map(Enum::name))
            .contains(filter.name());
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
     * Verifies that a filtered value rejects the other game modes.
     */
    @Test
    void shouldRejectOtherGameModesWhenFiltered() {
        assertThat(
            ChallengeGameMode.DEATHMATCH.matches(GameMode.TEAM_DEATHMATCH)
        ).isFalse();
        assertThat(
            ChallengeGameMode.DEATHMATCH.matches(GameMode.DEATHMATCH)
        ).isTrue();
    }

    /**
     * Verifies that no filter designates a mode synchronization does not import.
     *
     * <p>A challenge filtered on an unimported mode can never progress: it would occupy one of the
     * four weekly difficulty slots and stay at zero for every player.
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
        assertThat(GameMode.valueOf(filter.name()).isImportEligible()).isTrue();
    }

    /**
     * Verifies that an unresolved game mode never satisfies a filtered value.
     */
    @Test
    void shouldRejectNullGameModeWhenFiltered() {
        assertThat(ChallengeGameMode.COMPETITIVE.matches(null))
            .isFalse();
    }
}

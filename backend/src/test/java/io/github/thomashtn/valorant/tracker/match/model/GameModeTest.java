package io.github.thomashtn.valorant.tracker.match.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for {@link GameMode}.
 */
class GameModeTest {

    /**
     * Modes the tracker follows and stores.
     */
    private static final Set<GameMode> IMPORTED = EnumSet.of(
        GameMode.COMPETITIVE,
        GameMode.UNRATED,
        GameMode.PREMIER,
        GameMode.DEATHMATCH,
        GameMode.SPIKE_RUSH,
        GameMode.SKIRMISH,
        GameMode.TEAM_DEATHMATCH,
        GameMode.OTHER
    );

    /**
     * Verifies the exact set of modes synchronization stores.
     *
     * <p>Pinned rather than derived, so a mode added for a new Riot queue fails this test until
     * someone decides whether it belongs in the tracker. Silently importing it would change what
     * challenges count, and silently ignoring it would lose the matches for good.
     */
    @Test
    void shouldImportOnlyTheFollowedGameModes() {
        Set<GameMode> imported = EnumSet.noneOf(GameMode.class);
        Set<GameMode> excluded = EnumSet.noneOf(GameMode.class);

        for (GameMode gameMode : GameMode.values()) {
            if (gameMode.isImportEligible()) {
                imported.add(gameMode);
            } else {
                excluded.add(gameMode);
            }
        }

        assertThat(imported).isEqualTo(IMPORTED);
        assertThat(excluded).containsExactlyInAnyOrder(
            GameMode.SWIFTPLAY,
            GameMode.NEW_MAP,
            GameMode.ESCALATION,
            GameMode.CUSTOM
        );
    }

    /**
     * Verifies that an unclassified queue is stored rather than dropped.
     *
     * <p>Henrik lags behind Riot releases, so a mode that matters may surface as an unknown queue
     * first. Importing it keeps the raw slug available for a later reclassification.
     */
    @Test
    void shouldImportAnUnclassifiedQueue() {
        assertThat(GameMode.OTHER.isImportEligible()).isTrue();
    }

    /**
     * Verifies that Henrik identifiers resolve to the expected mode.
     *
     * @param rawIdentifier raw Henrik queue identifier
     * @param expected expected game mode
     */
    @ParameterizedTest
    @CsvSource({
        "competitive, COMPETITIVE",
        "unrated, UNRATED",
        "swiftplay, SWIFTPLAY",
        "newmap, NEW_MAP",
        "spikerush, SPIKE_RUSH",
        "deathmatch, DEATHMATCH",
        "hurm, TEAM_DEATHMATCH",
        "Team Deathmatch, TEAM_DEATHMATCH",
        "ggteam, ESCALATION",
        "skirmish_2v2, SKIRMISH",
        "premier, PREMIER",
        "custom, CUSTOM"
    })
    void shouldResolveKnownIdentifiers(String rawIdentifier, GameMode expected) {
        assertThat(GameMode.fromIdentifier(rawIdentifier)).contains(expected);
    }

    /**
     * Verifies that an unknown or blank identifier stays unresolved.
     *
     * <p>Empty rather than {@link GameMode#OTHER}, so the caller can try the next identifier Henrik
     * exposes before giving up.
     */
    @Test
    void shouldNotResolveAnUnknownIdentifier() {
        assertThat(GameMode.fromIdentifier("valorant_royale")).isEmpty();
        assertThat(GameMode.fromIdentifier("")).isEmpty();
        assertThat(GameMode.fromIdentifier(null)).isEmpty();
    }

    /**
     * Verifies that per-round averages are only meaningful for scored-round modes.
     */
    @Test
    void shouldDeclareRoundBasedModes() {
        assertThat(GameMode.COMPETITIVE.isRoundBased()).isTrue();
        assertThat(GameMode.SKIRMISH.isRoundBased()).isTrue();
        assertThat(GameMode.DEATHMATCH.isRoundBased()).isFalse();
        assertThat(GameMode.TEAM_DEATHMATCH.isRoundBased()).isFalse();
    }
}

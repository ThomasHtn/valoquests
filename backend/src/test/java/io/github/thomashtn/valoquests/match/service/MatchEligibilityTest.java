package io.github.thomashtn.valoquests.match.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests the single rule deciding whether a played match counts anywhere in the competition.
 */
class MatchEligibilityTest {

    /**
     * Rule under test.
     */
    private final MatchEligibility eligibility = new MatchEligibility();

    /**
     * Verifies that a real match played in a priced mode counts.
     */
    @Test
    void shouldCountARealMatchInAScoredMode() {
        assertThat(eligibility.isEligible(playerMatch(GameMode.COMPETITIVE, 24, 250, MatchResult.WIN)))
            .isTrue();
    }

    /**
     * Verifies that every mode the scoring table prices is counted.
     *
     * <p>A mode priced by {@code DefaultScoringRuleset#matchDamage} but rejected here would count as no day played.
     *
     * @param gameMode mode under test
     */
    @ParameterizedTest
    @EnumSource(
        value = GameMode.class,
        names = {"COMPETITIVE", "UNRATED", "SPIKE_RUSH", "DEATHMATCH", "TEAM_DEATHMATCH", "SKIRMISH", "PREMIER"}
    )
    void shouldCountEveryPricedMode(GameMode gameMode) {
        assertThat(eligibility.isEligible(playerMatch(gameMode, 13, 180, MatchResult.WIN))).isTrue();
    }

    /**
     * Verifies that an unrecognized queue never counts, however real the match looks.
     *
     * <p>{@link GameMode#OTHER} is imported for later reclassification but has no price, so it must not count as
     * a day played or progress mode-free challenges.
     */
    @Test
    void shouldNotCountAnUnrecognizedQueue() {
        assertThat(eligibility.isEligible(playerMatch(GameMode.OTHER, 24, 250, MatchResult.WIN)))
            .isFalse();
    }

    /**
     * Verifies that a match without a played round never counts.
     */
    @Test
    void shouldNotCountAMatchWithoutAPlayedRound() {
        assertThat(eligibility.isEligible(playerMatch(GameMode.COMPETITIVE, 0, 250, MatchResult.LOSS)))
            .isFalse();
    }

    /**
     * Verifies that a match the player scored nothing in never counts.
     */
    @Test
    void shouldNotCountAScorelessMatch() {
        assertThat(eligibility.isEligible(playerMatch(GameMode.COMPETITIVE, 24, 0, MatchResult.LOSS)))
            .isFalse();
    }

    /**
     * Builds one played match.
     *
     * @param gameMode     mode the match was played in
     * @param roundsPlayed rounds the player took part in
     * @param score        combat score the player finished on
     * @param result       team result reported for the match
     * @return player-match fixture
     */
    private PlayerMatch playerMatch(
        GameMode gameMode,
        int roundsPlayed,
        int score,
        MatchResult result
    ) {
        ValorantMatch match = new ValorantMatch();
        match.setGameMode(gameMode);

        PlayerMatch playerMatch = new PlayerMatch();
        playerMatch.setMatch(match);
        playerMatch.setRoundsPlayed(roundsPlayed);
        playerMatch.setScore(score);
        playerMatch.setResult(result);

        return playerMatch;
    }
}
